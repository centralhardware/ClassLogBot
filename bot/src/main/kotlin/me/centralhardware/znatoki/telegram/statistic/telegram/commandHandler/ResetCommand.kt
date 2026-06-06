package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import me.centralhardware.telegram.conversation.ConversationState
import me.centralhardware.znatoki.telegram.statistic.extensions.userId

fun BehaviourContext.resetCommand() = onCommand("reset") {
    val userId = it.userId()
    val cancelled = ConversationState.cancel(userId)
    
    val message = if (cancelled) {
        "Текущая операция отменена и состояние сброшено"
    } else {
        "Нет активных операций"
    }
    
    sendMessage(it.chat, message, replyMarkup = ReplyKeyboardRemove())
}

