package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper

suspend fun paymentRestoreCallback(query: DataCallbackQuery) {
    val id = query.data.replace("paymentRestore-", "").toInt()

    if (PaymentMapper.getOrgById(id) != UserMapper.getById(query.from.id.chatId.long)?.organizationId) {
        bot.sendMessage(query.from, "Доступ запрещен")
        return
    }

    PaymentMapper.setDelete(id, false)

    query.messageDataCallbackQueryOrNull() ?.message ?. let {
        bot.edit(it, replyMarkup = inlineKeyboard(){
            row { dataInlineButton("удалить", "paymentDelete-$id") }
        })
    }
}