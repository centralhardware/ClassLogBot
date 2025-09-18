package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionTyping
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.chat.PreviewChat
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.ensureNoActiveFsm
import me.centralhardware.znatoki.telegram.statistic.user
import java.io.File

private suspend fun BehaviourContext.createReport(
    userId: Long,
    chat: PreviewChat,
    getTime: suspend (Long) -> List<File>
) {
    if (!ensureNoActiveFsm(userId, chat)) {
        return
    }

    if (data.user.hasAdminPermission()) {
        ServiceMapper.getIds().forEach { getTime.invoke(it).forEach { send(chat.id, it) } }
        return
    }

    getTime.invoke(userId).forEach {
        send(chat.id, it)
        it.delete()
    }
}

suspend fun BehaviourContext.send(id: IdChatIdentifier, file: File) {
    sendDocument(id, InputFile(file))
    file.delete()
}

fun BehaviourContext.reportCommand() = onCommand(Regex("report")) {
    sendActionTyping(it.chat)
    createReport(it.userId(), it.chat) { id ->
        sendActionUploadDocument(it.chat)
        ReportService.getReportsCurrent(id)
    }
}

fun BehaviourContext.reportPreviousCommand() = onCommand(Regex("reportPrevious|reportprevious")) {
    sendActionTyping(it.chat)
    createReport(it.userId(), it.chat) { id ->
        sendActionUploadDocument(it.chat)
        ReportService.getReportPrevious(id)
    }
}