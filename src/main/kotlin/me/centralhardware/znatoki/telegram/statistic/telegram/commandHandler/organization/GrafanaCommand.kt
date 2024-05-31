package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.organization

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.ConfigMapper

suspend fun grafanaCommand(message: CommonMessage<TextContent>){
    bot.sendMessage(
        message.chat,
        """
            адрес: ${ConfigMapper.grafanaUrl()}
            пользователь: ${ConfigMapper.grafanaUsername()}
            пароль: ${ConfigMapper.grafanaPassword()}
        """.trimIndent())
}