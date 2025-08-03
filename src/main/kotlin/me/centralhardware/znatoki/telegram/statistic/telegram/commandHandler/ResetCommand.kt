package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage

suspend fun BehaviourContext.resetCommand() = onCommand("reset") {
    Storage.remove(it.userId())
    sendMessage(it.chat, "Состояние сброшено", replyMarkup = ReplyKeyboardRemove())
}

