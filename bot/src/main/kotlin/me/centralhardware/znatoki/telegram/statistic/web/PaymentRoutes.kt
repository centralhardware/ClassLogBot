package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyConfig
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val HM = DateTimeFormatter.ofPattern("HH:mm")

@Serializable
data class PaymentInput(val tutor: Long? = null, val student: Int, val subject: Long, val amount: Int)

fun PaymentRow.toDto(names: Map<Int, StudentName>) = PaymentDto(
    id = id,
    date = dateTime.toLocalDate().toString(),
    time = dateTime.toLocalTime().format(HM),
    student = names[student]?.ref() ?: StudentRefDto(student, "#$student"),
    subject = subject,
    amount = amount,
    tutor = tutor,
    deleted = deleted,
    hasPhoto = photo != null,
    addedBy = addedBy,
    source = source,
)

fun paymentTitle(p: PaymentRow, names: Map<Int, StudentName>) = (names[p.student]?.short ?: "#${p.student}") + " · " + rub(p.amount)

fun Route.paymentRoutes(ctx: WebContext) {
    requires(Access.READ_PAYMENTS) {
        apiGet("/payments") { c ->
            val month = c.monthParam(call.request.queryParameters["month"])
            val names = WebRepo.studentNames()
            call.respond(WebRepo.payments(month.start(), month.end(), c.tutorFilter(call.request.queryParameters["tutor"])).map { it.toDto(names) })
        }
        apiGet("/payments/{id}") { c ->
            val p = WebRepo.payment(call.parameters["id"]?.toIntOrNull() ?: notFound()) ?: notFound()
            if (!c.seesAll && p.tutor != c.id) forbidden()
            call.respond(p.toDto(WebRepo.studentNames()))
        }
    }


    requires(Access.PAYMENT) {
        post("/payments") {
            val c = call.caller
            val (raw, photo) = call.receiveForm()
            val input = WebServer.json.decodeFromString<PaymentInput>(raw)
            photo ?: badRequest("Приложите чек.")
            val tutorId = input.tutor?.takeIf { it != c.id }?.also { if (!c.canPaymentForOthers) forbidden("Нет права вносить оплаты за других.") } ?: c.id
            TutorMapper.findByIdOrNull(TutorId(tutorId)) ?: badRequest("Репетитор не найден.")
            val subjects = subjectNames()
            if (input.subject !in subjects) badRequest("Предмет не найден.")
            if (!Amount.validate(input.amount)) badRequest("Сумма должна быть больше нуля.")
            val names = WebRepo.studentNames()
            if (names[input.student]?.deleted != false) badRequest("Ученик не найден или удалён.")

            val now = LocalDateTime.now()
            val key = MinioService.upload(photo, now).getOrElse { throw HttpError(HttpStatusCode.BadGateway, "Не удалось сохранить чек. Попробуйте ещё раз.") }
            val payment = Payment(
                dateTime = now, tutorId = TutorId(tutorId), studentId = input.student.toStudentId(), amount = Amount(input.amount),
                subjectId = SubjectId(input.subject), photoReport = key,
                addedByTutorId = if (tutorId != c.id) c.tutorId else null, dataSource = DataSource.WEB,
            )
            val id = runCatching { PaymentMapper.insert(payment) }.onFailure { MinioService.delete(key) }.getOrThrow()
            val row = WebRepo.payment(id.id)!!
            AuditService.log(
                c.id, "Создано", "payment", id.id, paymentTitle(row, names),
                listOf(Change("сумма", null, rub(input.amount)), Change("чек", null, "приложен")),
                DataSource.WEB, input.student, input.subject,
            )
            ctx.notifier.launch {
                sendWebPaymentLog(payment, id, if (tutorId != c.id) c.tutorId else null)
                if (FireflyConfig.enabled) FireflyService.exportPayment(payment.copy(id = id))
            }
            call.respond(row.toDto(names))
        }
    }

    patch("/payments/{id}") {
        val c = call.caller
        val p = WebRepo.payment(call.parameters["id"]?.toIntOrNull() ?: notFound()) ?: notFound()
        c.editDenied(p.month, p.tutor, c.canPayment)?.let { forbidden(it) }
        val patch = call.receive<PaymentPatch>()
        val names = WebRepo.studentNames()
        val subjects = subjectNames()
        val tutors = TutorMapper.getAll().associate { it.id.id to it.name }
        val fields = mutableMapOf<String, Any?>()
        val changes = mutableListOf<Change>()
        var action = "Изменено"
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
            if (it !in tutors) badRequest("Репетитор не найден.")
            fields["tutor_id"] = it; changes += Change("принял", tutors[p.tutor], tutors[it])
        }
        patch.deleted?.takeIf { it != p.deleted }?.let {
            fields["is_deleted"] = it; changes += Change("удалена", p.deleted, it); action = if (it) "Удалено" else "Восстановлено"
        }
        WebRepo.updatePayment(p.id, fields)
        val after = WebRepo.payment(p.id)!!
        if (changes.isNotEmpty()) {
            AuditService.log(c.id, action, "payment", p.id, paymentTitle(after, names), changes, DataSource.WEB, p.student, after.subject)
            if (c.isAdmin && p.month != YearMonth.now()) ctx.alerts.closedMonthEdit(c.id, c.tutor.name, "оплата ${paymentTitle(after, names)}", p.month.toString())
        }
        call.respond(after.toDto(names))
    }
}
