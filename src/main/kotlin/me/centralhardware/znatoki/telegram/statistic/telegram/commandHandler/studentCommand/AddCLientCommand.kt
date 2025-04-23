package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.buildClientFsm

suspend fun BehaviourContext.addClientCommand() = onCommand(Regex("addPupil|addpupil")) {
    Trace.save("addClient", mapOf())
    if (Storage.contain(it.userId())) {
        bot.sendMessage(it.chat, "Сначала сохраните текущую запись")
        return@onCommand
    }
    Storage.create(it.userId(), buildClientFsm(allUpdatesFlow, it.userId()))
}
