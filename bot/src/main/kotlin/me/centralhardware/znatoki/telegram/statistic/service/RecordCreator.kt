package me.centralhardware.znatoki.telegram.statistic.service

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotliquery.Session
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.extensions.transaction
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyConfig
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.web.WebRepo
import me.centralhardware.znatoki.telegram.statistic.web.rub
import me.centralhardware.znatoki.telegram.statistic.web.sendLessonLog
import me.centralhardware.znatoki.telegram.statistic.web.sendPaymentLog
import me.centralhardware.znatoki.telegram.statistic.web.subjectNames
import java.time.LocalDateTime

/*
 * Creating a lesson or a payment, the same way whether it came from the bot's /addlesson and /addpayment
 * or from the web app: the same checks, one transaction for the rows and their audit entry, the photo
 * removed if that fails, and the log-chat post (and Firefly export) after the commit. The callers only
 * collect the input and turn [InvalidRecord] into their own kind of error message.
 */

/** The input breaks a rule; [message] is shown to the person entering it. */
class InvalidRecord(message: String) : RuntimeException(message)

private fun invalid(message: String): Nothing = throw InvalidRecord(message)

/** After-commit work (log chat, Firefly): off the caller, never failing it. */
private val afterCommit = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun TelegramBot.later(what: String, block: suspend TelegramBot.() -> Unit) {
    afterCommit.launch { runCatching { block() }.onFailure { KSLog.warning("RecordCreator: $what failed", it) } }
}

data class NewLesson(
    val tutor: Tutor,
    val subject: SubjectId,
    val students: List<Int>,
    val amount: Int,
    val dateTime: LocalDateTime,
    val photoKey: String,
    /** Who entered it; differs from [tutor] when entered on someone's behalf. */
    val enteredBy: TutorId,
    /** Admins may use a subject not assigned to the tutor. */
    val byAdmin: Boolean,
    val source: DataSource,
)

data class NewPayment(
    val tutor: TutorId,
    val student: Int,
    val subject: SubjectId,
    val amount: Int,
    val dateTime: LocalDateTime,
    val photoKey: String,
    val enteredBy: TutorId,
    val source: DataSource,
)

object LessonCreator {

    context(_: Session)
    fun validate(l: NewLesson) {
        val subject = WebRepo.subjects().find { it.id == l.subject.id } ?: invalid("Предмет не найден.")
        if (!l.byAdmin && l.tutor.subjects.isNotEmpty() && l.subject !in l.tutor.subjects) invalid("Этот предмет не закреплён за репетитором.")
        if (l.students.isEmpty()) invalid("Выберите ученика.")
        if (l.students.distinct().size > 1 && !subject.group) invalid("${subject.name}: только индивидуальные занятия.")
        if (!Amount.validate(l.amount)) invalid("Стоимость должна быть больше нуля.")
        val names = WebRepo.studentNames()
        l.students.forEach { if (names[it]?.deleted != false) invalid("Ученик не найден или удалён.") }
    }

    /**
     * Validates [l], writes one row per student and the audit entry in one transaction ([alsoInTransaction]
     * joins it, e.g. the web's schedule mark), deletes the photo if any of it fails, then posts to the log chat.
     */
    context(_: Session)
    fun create(
        bot: TelegramBot,
        l: NewLesson,
        extraChanges: List<Change> = emptyList(),
        alsoInTransaction: (List<Lesson>) -> Unit = {},
    ): List<Lesson> {
        val rows = runCatching {
            validate(l)
            transaction {
                val now = LocalDateTime.now()
                val id = LessonId.random()
                val addedBy = l.enteredBy.takeIf { it != l.tutor.id }
                val rows = l.students.distinct().map {
                    Lesson(
                        id = id, dateTime = l.dateTime, updateTime = now, tutorId = l.tutor.id, subjectId = l.subject,
                        studentId = it.toStudentId(), _amount = Amount(l.amount), photoReport = l.photoKey,
                        addedByTutorId = addedBy, dataSource = l.source,
                    )
                }
                LessonMapper.insertAll(rows)
                alsoInTransaction(rows)
                AuditService.log(
                    l.enteredBy.id, "Создано", "lesson", id.id, title(l),
                    listOfNotNull(
                        Change("стоимость", null, l.amount),
                        Change("учеников", null, rows.size),
                        Change("репетитор", null, l.tutor.name).takeIf { addedBy != null },
                    ) + extraChanges,
                    l.source, l.students.first(), l.subject.id,
                )
                rows
            }
        }.onFailure { MinioService.delete(l.photoKey) }.getOrThrow()
        val addedBy = l.enteredBy.takeIf { it != l.tutor.id }
        bot.later("lesson log") { db { sendLessonLog(rows, addedBy, l.source) } }
        return rows
    }

    context(_: Session)
    fun title(l: NewLesson): String {
        val names = WebRepo.studentNames()
        return l.students.distinct().joinToString(", ") { names[it]?.short ?: "#$it" } + " · " + (subjectNames()[l.subject.id] ?: "")
    }
}

object PaymentCreator {

    context(_: Session)
    fun validate(p: NewPayment) {
        if (p.subject.id !in subjectNames()) invalid("Предмет не найден.")
        if (!Amount.validate(p.amount)) invalid("Сумма должна быть больше нуля.")
        if (WebRepo.studentNames()[p.student]?.deleted != false) invalid("Ученик не найден или удалён.")
    }

    /** Validates [p], writes it and its audit entry in one transaction, then the log chat and the Firefly export. */
    context(_: Session)
    fun create(bot: TelegramBot, p: NewPayment): Pair<PaymentId, Payment> {
        val payment = Payment(
            dateTime = p.dateTime, tutorId = p.tutor, studentId = p.student.toStudentId(),
            amount = Amount(p.amount), subjectId = p.subject, photoReport = p.photoKey,
            addedByTutorId = p.enteredBy.takeIf { it != p.tutor }, dataSource = p.source,
        )
        val id = runCatching {
            validate(p)
            transaction {
                PaymentMapper.insert(payment).also { id ->
                    val name = WebRepo.studentNames()[p.student]?.short ?: "#${p.student}"
                    AuditService.log(
                        p.enteredBy.id, "Создано", "payment", id.id, "$name · ${rub(p.amount)}",
                        listOf(Change("сумма", null, rub(p.amount)), Change("чек", null, "приложен")),
                        p.source, p.student, p.subject.id,
                    )
                }
            }
        }.onFailure { MinioService.delete(p.photoKey) }.getOrThrow()
        val saved = payment.copy(id = id)
        bot.later("payment log / Firefly export") {
            db {
                sendPaymentLog(saved, id, saved.addedByTutorId, p.source)
                // A failure here is retried by FireflyService.retryPending.
                if (FireflyConfig.enabled) FireflyService.exportPayment(saved)
            }
        }
        return id to saved
    }
}
