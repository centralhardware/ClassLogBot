package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.PaymentFsm
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.startPaymentFsm
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class AddPaymentCommand(
    private val storage: Storage, sender: TelegramSender, userMapper: UserMapper
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        if (storage.contain(update.userId())) {
            sender.sendText("Сначала сохраните текущую запись", update.userId(), false)
            return
        }

        storage.create(update.userId(), PaymentFsm(startPaymentFsm(update)))
    }

    override fun isAcceptable(data: String): Boolean = data.equals("/addPayment", true)

    override fun getRequiredRole(): Role = Role.READ

}