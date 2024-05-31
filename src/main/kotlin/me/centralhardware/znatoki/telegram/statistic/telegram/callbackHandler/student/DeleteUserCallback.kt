package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper

fun deleteUserCallback(query: DataCallbackQuery) {
    ClientMapper.delete(query.data.replace("delete_user", "").toInt())
}