package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.PreviewChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import me.centralhardware.znatoki.telegram.statistic.extensions.userId

suspend fun BehaviourContext.ensureNoActiveFsm(message: CommonMessage<*>): Boolean {
    val userId = message.userId()
    if (Storage.contain(userId)) {
        sendMessage(message.chat, "Сначала сохраните текущую запись")
        return false
    }
    return true
}

suspend fun BehaviourContext.ensureNoActiveFsm(userId: Long, chat: PreviewChat): Boolean {
    if (Storage.contain(userId)) {
        sendMessage(chat, "Сначала сохраните текущую запись")
        return false
    }
    return true
}
