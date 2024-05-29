package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.i18n.load
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper

suspend fun userInfoCommand(message: CommonMessage<TextContent>, args: Array<String>) {
    ClientMapper.findById(args.first().toInt())?.let { client ->
        val orgId = UserMapper.findById(message.userId())!!.organizationId
        if (client.organizationId != orgId) {
            bot.sendMessage(message.chat, "Доступ запрещен")
            return
        }

        bot.sendMessage(
            message.chat,
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!).mapNotNull { ServicesMapper.getNameById(it) }.toList()
            )
        )
    } ?: bot.sendMessage(message.chat, I18n.Message.PUPIL_NOT_FOUND.load())
}