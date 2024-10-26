package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import java.util.*
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper

suspend fun timeDeleteCallback(query: DataCallbackQuery) {
    val id = UUID.fromString(query.data.replace("timeDelete-", ""))

    ServiceMapper.setDeleted(id, true)
    PaymentMapper.setDeleteByTimeId(id, true)

    query.messageDataCallbackQueryOrNull()?.message?.let {
        bot.edit(
            it,
            replyMarkup = inlineKeyboard { row { dataButton("восстановить", "timeRestore-$id") } }
        )
    }
}
