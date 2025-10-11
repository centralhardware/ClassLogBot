package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startPaymentFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.ensureNoActiveFsm

fun BehaviourContext.addPaymentForOtherCommand() = onCommand(Regex("addPaymentForOther|addpaymentforother")) {
    if (!ensureNoActiveFsm(it.userId())) {
        return@onCommand
    }

    Storage.create(it.userId(), startPaymentFsm(it, canAddForOthers = true))
}
