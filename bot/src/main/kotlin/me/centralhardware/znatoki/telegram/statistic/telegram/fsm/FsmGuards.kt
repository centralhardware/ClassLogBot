package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.toChatId

suspend fun BehaviourContext.ensureNoActiveFsm(userId: Long): Boolean {
    if (Storage.contain(userId)) {
        sendMessage(userId.toChatId(), "Сначала сохраните текущую запись")
        return false
    }
    return true
}