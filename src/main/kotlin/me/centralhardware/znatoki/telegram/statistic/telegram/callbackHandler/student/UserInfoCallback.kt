package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

suspend fun userInfoCallback(query: DataCallbackQuery) {
    ClientMapper.findById(query.data.replace("userInfoCallback", "").toInt())?.let { client ->
        Trace.save("getUserInfo", mapOf("id" to client.id.toString()))
        val info =
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!)
                    .mapNotNull { ServicesMapper.getNameById(it) }
                    .toList()
            )
        bot.sendMessage(query.from, info, parseMode = MarkdownParseMode)
    } ?: bot.sendMessage(query.from, "Пользователь не найден")
}
