package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendActionTyping
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import java.time.YearMonth

fun BehaviourContext.reportMonthCommand() = onCommandWithArgs(Regex("reportMonth|reportmonth")) { message, args ->
    if (args.size != 2) {
        reply(message, "Использование: /reportMonth <год> <месяц>\nПример: /reportMonth 2024 12")
        return@onCommandWithArgs
    }

    val year = args[0].toIntOrNull()
    val month = args[1].toIntOrNull()

    if (year == null || month == null || month !in 1..12) {
        reply(message, "Неверный формат. Год и месяц должны быть числами, месяц от 1 до 12")
        return@onCommandWithArgs
    }

    val yearMonth = YearMonth.of(year, month)

    sendActionTyping(message.chat)

    LessonMapper.getTutorIds().forEach { tutorId ->
        ReportService.getReportByMonth(tutorId, yearMonth).forEach { report ->
            send(message.userId(), report)
        }
    }
}
