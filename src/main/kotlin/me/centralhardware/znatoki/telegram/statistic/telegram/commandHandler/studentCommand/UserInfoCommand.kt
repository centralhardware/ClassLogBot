package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

suspend fun userInfoCommand(message: CommonMessage<TextContent>, args: Array<String>) {
    ClientMapper.findById(args.first().toInt())?.let { client ->
        Trace.save("userInfoCommand", mapOf("id" to client.id.toString()))
        bot.sendMessage(
            message.chat,
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!)
                    .mapNotNull { ServicesMapper.getNameById(it) }
                    .toList()
            ),
        )
    } ?: bot.sendMessage(message.chat, "Ученик не найден")
}
