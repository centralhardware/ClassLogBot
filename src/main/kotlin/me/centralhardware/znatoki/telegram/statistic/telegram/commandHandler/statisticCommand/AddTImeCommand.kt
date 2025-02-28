package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.longPollingFlow
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.buildTimeFsm

suspend fun BehaviourContext.addTimeCommand() = onCommand(Regex("addTime|addtime")) {
    if (Storage.contain(it.userId())) {
        bot.sendMessage(it.chat, "Сначала сохраните текущую запись")
        return@onCommand
    }

    Trace.save("addTime", mapOf())
    Storage.create(it.userId(), buildTimeFsm(longPollingFlow(), it.userId()))
}
