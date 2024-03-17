package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.createClientFsm
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class AddClientCommand(
    private val storage: Storage,
    sender: TelegramSender,
    userMapper: UserMapper
) : CommandHandler(
    sender, userMapper
) {

    override fun handle(update: Update) {
        if (storage.contain(update.userId())) {
            sender.sendText("Сначала сохраните текущую запись", update.userId(), false)
            return
        }
        storage.create(update.userId(), createClientFsm(), ClientBuilder())
        storage.process(update.userId(), update)

    }

    override fun isAcceptable(data: String): Boolean = data.equals("/addPupil", ignoreCase = true)

    override fun getRequiredRole(): Role = Role.READ_WRITE
}