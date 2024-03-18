package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class ResetCommand(private val storage: Storage, sender: TelegramSender, userMapper: UserMapper) : CommandHandler(
    sender,
    userMapper
) {

    override fun handle(update: Update) {
        storage.remove(update.userId())
        sender.sendText("Состояние сброшено", update.userId())
    }

    override fun isAcceptable(data: String): Boolean = data.equals("/reset", ignoreCase = true)

    override fun getRequiredRole(): Role = Role.READ

}