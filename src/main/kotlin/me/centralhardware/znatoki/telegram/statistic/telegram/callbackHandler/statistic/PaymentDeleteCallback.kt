package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper

suspend fun paymentDeleteCallback(query: DataCallbackQuery){
    val id = query.data.replace("paymentDelete-", "").toInt()

    PaymentMapper.setDelete(id, true)

    query.messageDataCallbackQueryOrNull() ?.message ?. let {
        bot.edit(it, replyMarkup = inlineKeyboard {
            row { dataButton("восстановить", "paymentRestore-$id") }
        })
    }
}