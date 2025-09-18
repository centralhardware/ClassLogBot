package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

fun BehaviourContext.userInfoCallback() = onDataCallbackQuery(Regex($$"user_info\\d+$")) {
    ClientMapper.findById(it.data.replace("user_info", "").toInt())?.let { client ->
        val info =
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!)
                    .mapNotNull { ServicesMapper.getNameById(it) }
                    .toList()
            )
        sendMessage(it.from, info, parseMode = MarkdownParseMode)
    } ?: sendMessage(it.from, "Пользователь не найден")
}
