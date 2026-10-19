package me.centralhardware.znatoki.telegram.statistic.web.service

import kotliquery.Session
import io.ktor.http.HttpStatusCode
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.extensions.transaction
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyConfig
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.HttpError
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.forbidden
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound
import java.time.LocalDateTime
import java.time.YearMonth

/** Payments: the month list, the payment card with its edits, and the add-payment form. */
object PaymentService {

    context(_: Session)
    fun list(c: Caller, month: YearMonth, tutor: Long?): List<PaymentDto> {
        val names = WebRepo.studentNames()
        return WebRepo.payments(month.start(), month.end(), tutor, withAdded = !c.seesAll).map { it.toDto(names) }
    }

    context(_: Session)
    fun get(c: Caller, id: Int): PaymentDto {
        val p = WebRepo.payment(id) ?: notFound()
        if (!c.seesAll && p.tutor != c.id && p.addedBy != c.id) forbidden()
        return p.toDto(WebRepo.studentNames())
    }

    context(_: Session)
    suspend fun create(c: Caller, input: PaymentInput, photo: ByteArray?): PaymentDto {
        photo ?: badRequest("Приложите чек.")
        val tutorId = targetTutor(c, input.tutor)
        validate(input)

        val payment = Payment(
            dateTime = LocalDateTime.now(), tutorId = TutorId(tutorId), studentId = input.student.toStudentId(),
            amount = Amount(input.amount), subjectId = SubjectId(input.subject),
            photoReport = upload(photo), addedByTutorId = c.tutorId.takeIf { it.id != tutorId }, dataSource = DataSource.WEB,
        )
        val names = WebRepo.studentNames()
        // The payment and its audit entry commit together; the receipt goes if they don't.
        val (id, row) = runCatching {
            transaction {
                val id = PaymentMapper.insert(payment)
                val row = WebRepo.payment(id.id)!!
                AuditService.log(
                    c.id, "Создано", "payment", id.id, paymentTitle(row, names),
                    listOf(Change("сумма", null, rub(input.amount)), Change("чек", null, "приложен")), DataSource.WEB, input.student, input.subject,
                )
                id to row
            }
        }.onFailure { MinioService.delete(payment.photoReport!!) }.getOrThrow()
        // Runs after the response, on its own session: the request's is closed by then.
        Notifications.background {
            db {
                sendPaymentLog(payment, id, payment.addedByTutorId, DataSource.WEB)
                if (FireflyConfig.enabled) FireflyService.exportPayment(payment.copy(id = id))
            }
        }
        return row.toDto(names)
    }

    context(_: Session)
    fun update(c: Caller, id: Int, patch: PaymentPatch): PaymentDto = transaction {
        val p = WebRepo.payment(id) ?: notFound()
        c.editDenied(p.month, p.tutor, c.canPayment)?.let { forbidden(it) }
        val (fields, changes) = diff(c, p, patch)
        WebRepo.updatePayment(p.id, fields)

        val names = WebRepo.studentNames()
        val after = WebRepo.payment(p.id)!!
        if (changes.isNotEmpty()) {
            val action = when (patch.deleted?.takeIf { it != p.deleted }) { true -> "Удалено"; false -> "Восстановлено"; null -> "Изменено" }
            AuditService.log(c.id, action, "payment", p.id, paymentTitle(after, names), changes, DataSource.WEB, p.student, after.subject)
            if (p.month != YearMonth.now()) AuditAlerts.closedMonthEdit(c.id, c.tutor.name, "оплата ${paymentTitle(after, names)}", p.month)
        }
        after.toDto(names)
    }

    context(_: Session)
    private fun targetTutor(c: Caller, requested: Long?): Long {
        val id = requested?.takeIf { it != c.id } ?: return c.id
        if (!c.canPaymentForOthers) forbidden("Нет права вносить оплаты за других.")
        TutorMapper.findByIdOrNull(TutorId(id)) ?: badRequest("Репетитор не найден.")
        return id
    }

    context(_: Session)
    private fun validate(input: PaymentInput) {
        if (input.subject !in subjectNames()) badRequest("Предмет не найден.")
        if (!Amount.validate(input.amount)) badRequest("Сумма должна быть больше нуля.")
        if (WebRepo.studentNames()[input.student]?.deleted != false) badRequest("Ученик не найден или удалён.")
    }

    private fun upload(photo: ByteArray): String =
        MinioService.upload(photo, LocalDateTime.now()).getOrElse { throw HttpError(HttpStatusCode.BadGateway, "Не удалось сохранить чек. Попробуйте ещё раз.") }

    /** The columns to update and the audit diff; changing who took the payment is for admins. */
    context(_: Session)
    private fun diff(c: Caller, p: PaymentRow, patch: PaymentPatch): Pair<Map<String, Any?>, List<Change>> {
        val subjects = subjectNames()
        val fields = mutableMapOf<String, Any?>()
        val changes = mutableListOf<Change>()
        patch.amount?.takeIf { it != p.amount }?.let {
            if (!Amount.validate(it)) badRequest("Сумма должна быть больше нуля.")
            fields["amount"] = it; changes += Change("сумма", rub(p.amount), rub(it))
        }
        patch.subject?.takeIf { it != p.subject }?.let {
            if (it !in subjects) badRequest("Предмет не найден.")
            fields["subject_id"] = it; changes += Change("предмет", subjects[p.subject], subjects[it])
        }
        patch.tutor?.takeIf { it != p.tutor }?.let {
            if (!c.isAdmin) forbidden("Сменить принявшего оплату может только админ.")
            val tutors = TutorMapper.getAll().associate { t -> t.id.id to t.name }
            if (it !in tutors) badRequest("Репетитор не найден.")
            fields["tutor_id"] = it; changes += Change("принял", tutors[p.tutor], tutors[it])
        }
        patch.deleted?.takeIf { it != p.deleted }?.let {
            fields["is_deleted"] = it; changes += Change("удалена", p.deleted, it)
        }
        return fields to changes
    }

    fun paymentTitle(p: PaymentRow, names: Map<Int, StudentName>) = (names[p.student]?.short ?: "#${p.student}") + " · " + rub(p.amount)
}

fun PaymentRow.toDto(names: Map<Int, StudentName>) = PaymentDto(
    id = id,
    date = dateTime.toLocalDate().toString(),
    time = dateTime.format(HM),
    student = names[student]?.ref() ?: StudentRefDto(student, "#$student"),
    subject = subject,
    amount = amount,
    tutor = tutor,
    deleted = deleted,
    hasPhoto = photo != null,
    addedBy = addedBy,
    source = source,
)
