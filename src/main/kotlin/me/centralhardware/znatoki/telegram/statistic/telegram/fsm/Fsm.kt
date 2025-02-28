package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.coroutines.runBlocking

fun <T : State> DefaultBehaviourContextWithFSM<T>.mapError(message: CommonMessage<MessageContent>): (String) -> Unit =
    { error ->
        runBlocking { sendTextMessage(message.chat, error) }
    }