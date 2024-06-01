package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper

suspend fun deleteUserCallback(query: DataCallbackQuery) {
    ClientMapper.delete(query.data.replace("delete_user", "").toInt())
    bot.sendMessage(query.from, I18n.Message.PUPIL_DELETED.load())
}