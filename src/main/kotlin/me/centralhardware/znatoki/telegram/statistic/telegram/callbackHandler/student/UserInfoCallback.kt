package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.i18n.load
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper

suspend fun userInfoCallback(query: DataCallbackQuery) {
    ClientMapper.findById(query.data.replace("/user_info", "").toInt())?.let { client ->
        if (client.organizationId != UserMapper.getById(query.from.id.chatId.long)?.organizationId) {
            bot.sendMessage(query.from,"Доступ запрещен")
            return
        }

        val info = client.getInfo(
            ServiceMapper.getServicesForClient(client.id!!)
                .mapNotNull { ServicesMapper.getNameById(it) }.toList()
        )
        bot.sendMessage(query.from, info, parseMode = MarkdownParseMode)
    }?: bot.sendMessage(query.from, I18n.Message.USER_NOT_FOUND.load())
}