package me.centralhardware.znatoki.telegram.statistic.telegram

import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentId
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change

/** Audit entries for changes made through the bot, so the web app's journal shows them too. */
object BotAudit {

    private fun name(id: StudentId) = runCatching {
        StudentMapper.findById(id).let { listOf(it.secondName, it.name).filter(String::isNotBlank).joinToString(" ") }
    }.getOrDefault("#${id.id}")

    fun lesson(userId: Long, action: String, id: LessonId, changes: List<Change>) {
        val lessons = LessonMapper.findById(id).ifEmpty { return }
        val title = lessons.joinToString(", ") { name(it.studentId) } + " · " +
            runCatching { SubjectMapper.getNameById(lessons.first().subjectId) }.getOrDefault("")
        AuditService.log(userId, action, "lesson", id.id, title, changes, DataSource.BOT, lessons.first().studentId.id, lessons.first().subjectId.id)
    }

    fun payment(userId: Long, action: String, id: PaymentId, payment: Payment, changes: List<Change>) =
        AuditService.log(userId, action, "payment", id.id, "${name(payment.studentId)} · ${payment.amount.amount} ₽", changes, DataSource.BOT,
            payment.studentId.id, payment.subjectId.id)

    fun student(userId: Long, action: String, id: StudentId, changes: List<Change>) =
        AuditService.log(userId, action, "student", id.id, name(id), changes, DataSource.BOT, id.id)
}
