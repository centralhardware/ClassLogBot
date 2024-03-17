package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.organization

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class GrafanaCommand(
    private val organizationMapper: OrganizationMapper, sender: TelegramSender, userMapper: UserMapper
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        val user = userMapper.getById(update.userId())!!

        val org = organizationMapper.getById(user.organizationId)!!
        sender.sendText("""
            адрес: ${org.grafanaUrl}
            пользователь: ${org.grafanaUsername}
            пароль: ${org.grafanaPassword}
        """.trimIndent(), update.userId())
    }

    override fun isAcceptable(data: String): Boolean = data.equals("/grafana", ignoreCase = true)

    override fun getRequiredRole(): Role = Role.ADMIN
}