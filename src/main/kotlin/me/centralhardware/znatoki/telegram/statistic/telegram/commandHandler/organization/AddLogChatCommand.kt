package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.organization

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class AddLogChatCommand(private val organizationMapper: OrganizationMapper,
                        sender: TelegramSender,
                        userMapper: UserMapper
) : CommandHandler(
    sender, userMapper
) {

    override fun handle(update: Update) {
        if (update.userId() > 0L){
            sender.sendText("Сначала добавте бота в чат, который хотите использовать для лога, затем выполните команду /join @@OrgStatisticBot", update.userId())
            return
        }

        val organization = organizationMapper.getByOwner(update.userId())!!

        when {
            organization.logChatId == update.userId() -> sender.sendText("Данный чат уже привязан к боту", update.userId())
            organization.logChatId != null -> sender.sendText("Чат для логирования уже добавлен", update.userId())
            else -> {
                organizationMapper.updateLogChat(organization.id, update.userId())
                sender.sendText("Чат сохранен. Теперь сюда будут приходить уведомления об действиях с ботом от всех сотрудников", update.userId())
            }
        }
    }

    override fun isAcceptable(data: String) = data.startsWith("/join")

    override fun getRequiredRole() = Role.ADMIN
}