package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionTyping
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.chat.PreviewChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import java.io.File
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.userId

private suspend fun createReport(userId: Long, chat: PreviewChat, getTime: (Long) -> List<File>) {
    if (Storage.contain(userId)) {
        return
    }

    if (UserMapper.hasAdminRight(userId)) {
        ServiceMapper.getIds().forEach { getTime.invoke(it).forEach { send(chat.id, it) } }
        return
    }

    getTime.invoke(userId).forEach { send(chat.id, it) }
}

suspend fun send(id: IdChatIdentifier, file: File) {
    bot.sendDocument(id, InputFile(file))
    file.delete()
}

suspend fun reportCommand(message: CommonMessage<TextContent>) {
    bot.sendActionTyping(message.chat)
    createReport(message.userId(), message.chat) {
        runBlocking { bot.sendActionUploadDocument(message.chat) }
        ReportService.getReportsCurrent(it)
    }
}

suspend fun reportPreviousCommand(message: CommonMessage<TextContent>) {
    bot.sendActionTyping(message.chat)
    createReport(message.userId(), message.chat) {
        runBlocking { bot.sendActionUploadDocument(message.chat) }
        ReportService.getReportPrevious(it)
    }
}
