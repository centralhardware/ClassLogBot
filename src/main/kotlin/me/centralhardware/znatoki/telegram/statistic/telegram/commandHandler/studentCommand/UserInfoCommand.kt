package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

suspend fun BehaviourContext.userInfoCommand() = onCommandWithArgs("i") { message, args ->
    ClientMapper.findById(args.first().toInt())?.let { client ->
        sendMessage(
            message.chat,
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!)
                    .mapNotNull { ServicesMapper.getNameById(it) }
                    .toList()
            ),
        )
    } ?: sendMessage(message.chat, "Ученик не найден")
}
