package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import java.util.*

suspend fun timeDeleteCallback(query: DataCallbackQuery) {
    val id = UUID.fromString(query.data.replace("timeDelete-", ""))

    if (ServiceMapper.getOrgId(id) != UserMapper.findById(query.from.id.chatId.long)?.organizationId) {
        bot.sendMessage(query.from,"Доступ запрещен")
        return
    }

    ServiceMapper.setDeleted(id, true)
    PaymentMapper.setDeleteByTimeId(id, true)

    query.messageDataCallbackQueryOrNull() ?.message ?. let {
        bot.edit(it, replyMarkup = inlineKeyboard {
            row { dataButton("восстановить", "timeRestore-$id") }
        })
    }
}