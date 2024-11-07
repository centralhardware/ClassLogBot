package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.TimeFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startTimeFsm

suspend fun addTimeCommand(message: CommonMessage<TextContent>) {
    if (Storage.contain(message.userId())) {
        bot.sendMessage(message.chat, "Сначала сохраните текущую запись")
        return
    }

    Trace.save("addTime", mapOf())
    val builder = startTimeFsm(message)
    val fsm = TimeFsm(builder)
    Storage.create(message.userId(), fsm)
    if (builder.serviceId != null) {
        Storage.process(message)
    }
}
