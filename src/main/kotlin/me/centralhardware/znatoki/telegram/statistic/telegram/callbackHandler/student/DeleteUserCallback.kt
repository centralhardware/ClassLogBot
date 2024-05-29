package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.i18n.load
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper

suspend fun deleteUserCallback(query: DataCallbackQuery) {
    ClientMapper.findById(query.data.replace("/delete_user", "").toInt())?.let {
        if (it.organizationId != UserMapper.findById(query.from.id.chatId.long)?.organizationId){
            bot.sendMessage(query.from, "Доступ запрещен")
            return
        }

        it.deleted = true
        ClientMapper.save(it)
        bot.sendMessage(query.from, I18n.Message.PUPIL_DELETED.load())
    }
}