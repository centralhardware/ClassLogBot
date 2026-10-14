package me.centralhardware.znatoki.telegram.statistic.web

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.krontab.buildSchedule
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.webAppButton
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val YMD = DateTimeFormatter.BASIC_ISO_DATE

/**
 * Evening reminders from "Напоминания": at the hour a tutor chose, the slots of the day that are
 * still unmarked, and two days before the month ends a nudge to check lessons and hand in the report.
 */
suspend fun TelegramBot.scheduleReminders() {
    buildSchedule("0 0 20,21,22 * * *").asTzFlowWithDelays().collect {
        runCatching { sendReminders(LocalTime.now().hour) }.onFailure { KSLog.warning("Reminders failed", it) }
    }
}

private suspend fun TelegramBot.sendReminders(hour: Int) {
    val at = "%02d:00".format(hour)
    val today = LocalDate.now()
    val names = WebRepo.studentNames()
    val subjects = subjectNames()
    val url = AppUrl.get(this)
    val allSlots = WebRepo.slots(null, names)
    val marks = WebRepo.marks(today, today, allSlots.map { it.id }).map { it.slot }.toSet()

    TutorMapper.getAll()
        .filter { Permissions.WEB_INTERFACE in it.permissions && (Permissions.ADD_TIME in it.permissions || Permissions.ADMIN in it.permissions) }
        .forEach { tutor ->
            val r = WebRepo.reminders(tutor.id.id)
            if (r.at != at) return@forEach
            runCatching {
                if (r.evening) {
                    val pending = WebRepo.slotsOn(today, allSlots.filter { it.tutor == tutor.id.id })
                        .filter { it.id !in marks }.sortedBy { it.time }
                    if (pending.isNotEmpty()) {
                        val text = "🕘 Сегодня по расписанию не отмечено:\n" + pending.joinToString("\n") {
                            "${it.time} ${subjects[it.subject]} — ${it.students.joinToString(", ") { s -> s.fio }}"
                        }
                        sendTextMessage(tutor.id.toChatId(), text, replyMarkup = if (url == null) null else inlineKeyboard {
                            pending.forEach { s ->
                                if (url != null) row { webAppButton("${s.time} — внести или отметить «не было»", "$url/#slot=${s.id}") }
                            }
                        })
                    }
                }
                val month = YearMonth.from(today)
                if (r.monthEnd && month.atEndOfMonth().minusDays(2) == today) {
                    sendTextMessage(
                        tutor.id.toChatId(),
                        "📅 До конца месяца 2 дня. Проверьте занятия за ${monthTitle(month).lowercase()} и сдайте отчёт директору.",
                        replyMarkup = url?.let { inlineKeyboard { row { webAppButton("Открыть отчёт", "$it/#tab=report") } } },
                    )
                }
            }.onFailure { KSLog.warning("Reminder to ${tutor.id.id} failed", it) }
        }
}
