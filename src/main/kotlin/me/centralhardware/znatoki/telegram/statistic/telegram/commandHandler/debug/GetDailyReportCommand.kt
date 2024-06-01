package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug

import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.report.getReport
import me.centralhardware.znatoki.telegram.statistic.userId

suspend fun dailyReportCommand(message: CommonMessage<TextContent>, args: Array<String>){
    getReport(args.first().toLong(), message.userId())
}