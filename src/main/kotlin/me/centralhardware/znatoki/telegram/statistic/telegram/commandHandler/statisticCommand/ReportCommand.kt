package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.chat.PreviewChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import java.io.File

private suspend fun createReport(userId: Long, chat: PreviewChat, getTime: (Long) -> List<File>) {
    if (Storage.contain(userId)) {
        return
    }

    if (UserMapper.isAdmin(userId)) {
        ServiceMapper.getIds(UserMapper.getById(userId)!!.organizationId).forEach {
            getTime.invoke(it).forEach { send(chat.id, it) }
        }
        return
    }
    if (UserMapper.hasWriteRight(userId)) {
        getTime.invoke(userId).forEach {
            send(chat.id, it)
        }
    }
}

suspend fun send(id: IdChatIdentifier, file: File) {
    bot.sendActionUploadPhoto(id)
    bot.sendDocument(id, InputFile(file))
    file.delete()
}

suspend fun reportCommand(message: CommonMessage<TextContent>) {
    createReport(message.userId(), message.chat) { ReportService.getReportsCurrent(it) }
}

suspend fun reportPreviousCommand(message: CommonMessage<TextContent>) {
    createReport(message.userId(), message.chat) { ReportService.getReportPrevious(it) }
}