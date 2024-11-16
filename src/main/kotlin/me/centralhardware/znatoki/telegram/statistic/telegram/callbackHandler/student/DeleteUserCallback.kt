package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper

suspend fun BehaviourContext.deleteUserCallback() = onDataCallbackQuery(Regex("delete_user\\d+\$")) {
    val id = it.data.replace("delete_user", "").toInt()
    Trace.save("deleteUser", mapOf("id" to id.toString()))
    ClientMapper.delete(id)
    sendMessage(it.from, "Ученик удален")
}
