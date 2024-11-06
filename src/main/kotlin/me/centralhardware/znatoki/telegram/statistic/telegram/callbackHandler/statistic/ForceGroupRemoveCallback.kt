package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import java.util.UUID
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper

suspend fun forceGroupRemove(query: DataCallbackQuery) {
    val id = UUID.fromString(query.data.replace("forceGroupRemove-", ""))

    Trace.save("forceGroupRemove", mapOf("id" to id.toString()))
    ServiceMapper.setForceGroup(id, false)
    val times = ServiceMapper.findById(id)
    if (times.size > 1) {
        bot.answerCallbackQuery(query, "Занятие уже групповое", showAlert = true)
        return
    }

    val keyboard = inlineKeyboard {
        if (times.first().deleted) {
            row { dataButton("восстановить", "timeRestore-$id") }
        } else {
            row { dataButton("удалить", "timeDelete-$id") }
        }
        row { dataButton("сделать групповым занятием", "forceGroupAdd-$id") }
    }

    query.messageDataCallbackQueryOrNull()?.let { bot.edit(it.message, replyMarkup = keyboard) }
}
