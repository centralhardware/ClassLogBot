package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class StartCommand(
    private val organizationMapper: OrganizationMapper, sender: TelegramSender,
    userMapper: UserMapper
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        sender.sendText(
            """
                         Бот предназначенный для анализа вашего бизнеса.
                         Автор: @centralhardware
                         """.trimIndent(), update.userId()
        )
        if (!organizationMapper.exist(update.userId())) {
            sender.sendText(Config.Telegram.startTelegraph, update.userId())
        }
    }

    override fun isAcceptable(data: String): Boolean = data == "/start"

    override fun getRequiredRole(): Role? = null

}