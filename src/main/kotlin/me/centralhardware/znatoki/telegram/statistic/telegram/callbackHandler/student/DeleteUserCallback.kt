package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper

suspend fun deleteUserCallback(query: DataCallbackQuery) {
    val id = query.data.replace("delete_user", "").toInt()
    Trace.save("deleteUser", mapOf("id" to id.toString()))
    ClientMapper.delete(id)
    bot.sendMessage(query.from, "Ученик удален")
}
