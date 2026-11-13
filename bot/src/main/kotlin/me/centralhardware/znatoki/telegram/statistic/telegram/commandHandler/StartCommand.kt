package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDeepLink
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage

fun BehaviourContext.startCommand() {
    onCommand("start") {
        sendMessage(
            it.chat,
            """
            Бот для управления учениками, занятиями и оплатами.
            Автор: @centralhardware
            """.trimIndent(),
        )
    }

}
