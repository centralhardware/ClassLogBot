package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startClientFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.ensureNoActiveFsm

fun BehaviourContext.addClientCommand() = onCommand(Regex("addPupil|addpupil")) {
    if (!ensureNoActiveFsm(it)) {
        return@onCommand
    }
    Storage.create(it.userId(), startClientFsm(it))
}

