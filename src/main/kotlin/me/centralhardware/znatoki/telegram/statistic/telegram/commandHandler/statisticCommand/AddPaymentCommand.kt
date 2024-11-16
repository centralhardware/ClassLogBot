package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.PaymentFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startPaymentFsm

suspend fun BehaviourContext.addPaymentCommand() = onCommand(Regex("addPayment|addpayment")) {
    if (Storage.contain(it.userId())) {
        bot.sendMessage(it.chat, "Сначала сохраните текущую запись")
        return@onCommand
    }

    Trace.save("addPayment", mapOf())
    Storage.create(it.userId(), PaymentFsm(startPaymentFsm(it)))
}
