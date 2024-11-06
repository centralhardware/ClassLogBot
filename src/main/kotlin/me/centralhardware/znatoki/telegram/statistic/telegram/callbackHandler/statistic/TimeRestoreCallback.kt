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

suspend fun timeRestoreCallback(query: DataCallbackQuery) {
    val id = UUID.fromString(query.data.replace("timeRestore-", ""))

    Trace.save("restoreTime", mapOf("id" to id.toString()))
    ServiceMapper.setDeleted(id, false)

    query.messageDataCallbackQueryOrNull()?.message?.let {
        bot.edit(
            it,
            replyMarkup = inlineKeyboard { row { dataButton("удалить", "timeDelete-$id") } },
        )
    }
}
