package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug

import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.report.getReport

suspend fun dailyReportCommand(message: CommonMessage<TextContent>, args: Array<String>) {
    getReport(args.first().toLong(), message.userId())
}
