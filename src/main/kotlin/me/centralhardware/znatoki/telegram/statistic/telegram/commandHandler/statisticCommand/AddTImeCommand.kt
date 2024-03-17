package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.createTimeFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startTimeFsm
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class AddTimeCommand(
    private val storage: Storage,
    sender: TelegramSender,
    userMapper: UserMapper
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        val userId = update.userId()

        if (storage.contain(userId)) {
            sender.sendText("Сначала сохраните текущую запись", userId, false)
            return
        }


        storage.create(update.userId(), createTimeFsm(), startTimeFsm(update))
    }

    override fun isAcceptable(data: String): Boolean = data.equals("/addTime", ignoreCase = true)

    override fun getRequiredRole(): Role = Role.READ
}