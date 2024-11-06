package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import java.util.*
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper

suspend fun timeDeleteCallback(query: DataCallbackQuery) {
    val id = UUID.fromString(query.data.replace("timeDelete-", ""))

    Trace.save("deleteTime", mapOf("id" to id.toString()))
    ServiceMapper.setDeleted(id, true)
    val times = ServiceMapper.findById(id)

    val keyboard = inlineKeyboard {
        row { dataButton("восстановить", "timeRestore-$id") }
        if (times.size == 1) {
            if (times.first().forceGroup) {
                row { dataButton("сделать одиночным занятием", "forceGroupRemove-$id") }
            } else {
                row { dataButton("сделать групповым занятием", "forceGroupAdd-$id") }
            }
        }
    }

    query.messageDataCallbackQueryOrNull()?.message?.let { bot.edit(it, replyMarkup = keyboard) }
}
