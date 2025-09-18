package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.TimeFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startTimeFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.ensureNoActiveFsm

fun BehaviourContext.addTimeCommand() = onCommand(Regex("addTime|addtime")) {
    if (!ensureNoActiveFsm(it)) {
        return@onCommand
    }

    val builder = startTimeFsm(it)
    val fsm = TimeFsm(builder, this)
    Storage.create(it.userId(), fsm)
    if (builder.serviceId != null) {
        Storage.process(it)
    }
}
