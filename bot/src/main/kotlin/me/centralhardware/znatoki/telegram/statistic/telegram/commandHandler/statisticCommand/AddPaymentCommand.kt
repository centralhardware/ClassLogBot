package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.telegram.conversation.CANCEL
import me.centralhardware.telegram.conversation.ConversationState
import me.centralhardware.telegram.conversation.startConversation
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.conversation.createPayment

fun BehaviourContext.addPaymentCommand() = onCommand(Regex("addPayment|addpayment")) {
    val userId = it.userId()
    val chatId = it.chat.id
    
    if (ConversationState.hasActive(userId)) {
        sendTextMessage(
            chatId,
            "У вас уже есть активная операция. Используйте $CANCEL для отмены или завершите текущую операцию."
        )
        return@onCommand
    }

    startConversation(userId) {
        createPayment(it, canAddForOthers = false)
    }
}
