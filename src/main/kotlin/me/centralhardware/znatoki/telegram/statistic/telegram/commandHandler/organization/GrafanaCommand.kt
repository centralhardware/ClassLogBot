package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.organization

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.userId

suspend fun grafanaCommand(message: CommonMessage<TextContent>){
    val user = UserMapper.findById(message.userId())!!

    val org = OrganizationMapper.findById(user.organizationId)!!
    bot.sendMessage(
        message.chat,
        """
            адрес: ${org.grafanaUrl}
            пользователь: ${org.grafanaUsername}
            пароль: ${org.grafanaPassword}
        """.trimIndent())
}