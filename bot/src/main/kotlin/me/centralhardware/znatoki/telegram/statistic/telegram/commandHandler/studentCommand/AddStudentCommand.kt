package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.CANCEL
import me.centralhardware.znatoki.telegram.statistic.telegram.ConversationState
import me.centralhardware.znatoki.telegram.statistic.telegram.conversation.createStudent
import me.centralhardware.znatoki.telegram.statistic.telegram.startConversation

fun BehaviourContext.addStudentCommand() = onCommand(Regex("addStudent|addstudent")) {
    val userId = it.userId()
    val chatId = it.chat.id
    
    if (ConversationState.hasActiveConversation(userId)) {
        sendTextMessage(
            chatId,
            "У вас уже есть активная операция. Используйте $CANCEL для отмены или завершите текущую операцию."
        )
        return@onCommand
    }
    
    startConversation(userId, ConversationState.ConversationType.STUDENT) {
        createStudent(it)
    }
}

