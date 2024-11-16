package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

suspend fun BehaviourContext.startCommand() = onCommand("start") {
    Trace.save("startCommand", mapOf())
    bot.sendMessage(
        it.chat,
        """
                         Бот предназначенный для анализа вашего бизнеса.
                         Автор: @centralhardware
                         """
            .trimIndent(),
    )
}
