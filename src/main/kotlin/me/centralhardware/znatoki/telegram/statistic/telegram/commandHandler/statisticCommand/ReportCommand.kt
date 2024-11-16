package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionTyping
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.chat.PreviewChat
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import java.io.File

private suspend fun createReport(userId: Long, chat: PreviewChat, getTime: suspend (Long) -> List<File>) {
    if (Storage.contain(userId)) {
        return
    }

    if (UserMapper.hasAdminPermission(userId)) {
        ServiceMapper.getIds().forEach { getTime.invoke(it).forEach { send(chat.id, it) } }
        return
    }

    getTime.invoke(userId).forEach {
        send(chat.id, it)
        it.delete()
    }
}

suspend fun send(id: IdChatIdentifier, file: File) {
    bot.sendDocument(id, InputFile(file))
    file.delete()
}

suspend fun BehaviourContext.reportCommand() = onCommand(Regex("report")) {
    Trace.save("report", mapOf())
    sendActionTyping(it.chat)
    createReport(it.userId(), it.chat) { id ->
        sendActionUploadDocument(it.chat)
        ReportService.getReportsCurrent(id)
    }
}

suspend fun BehaviourContext.reportPreviousCommand() = onCommand(Regex("reportPrevious|reportprevious")) {
    Trace.save("reportPrevious", mapOf())
    sendActionTyping(it.chat)
    createReport(it.userId(), it.chat) { id ->
        sendActionUploadDocument(it.chat)
        ReportService.getReportPrevious(id)
    }
}