package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.i18n.load
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

suspend fun userInfoCallback(query: DataCallbackQuery) {
    ClientMapper.findById(query.data.replace("user_info", "").toInt())?.let { client ->
        val info = client.getInfo(
            ServiceMapper.getServicesForClient(client.id!!)
                .mapNotNull { ServicesMapper.getNameById(it) }.toList()
        )
        bot.sendMessage(query.from, info, parseMode = MarkdownParseMode)
    }?: bot.sendMessage(query.from, I18n.Message.USER_NOT_FOUND.load())
}