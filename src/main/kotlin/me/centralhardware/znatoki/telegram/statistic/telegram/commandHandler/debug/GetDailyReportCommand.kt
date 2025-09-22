package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.report.getReport

fun BehaviourContext.dailyReportCommand() = onCommandWithArgs("dailyReport") { message, args ->
    getReport(TutorId(args.first().toLong()), message.tutorId())
}
