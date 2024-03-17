package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User

@Component
class DeleteUserCallback(
    private val clientService: ClientService,
    sender: TelegramSender,
    telegramService: TelegramService,
    userMapper: UserMapper
): CallbackHandler(sender, telegramService, userMapper) {

    override fun handle(callbackQuery: CallbackQuery, from: User, data: String) {
        if (!telegramService.isAdmin(from.id)) {
            sender.sendMessageFromResource(I18n.Error.ACCESS_DENIED, from.id)
            return
        }

        clientService.findById(data.replace("/delete_user", "").toInt())?.let {
            if (it.organizationId != getTelegramUser(from)?.organizationId){
                sender.sendText("Доступ запрещен", from.id)
                return
            }

            it.deleted = true
            clientService.save(it)
            sender.sendMessageFromResource(I18n.Message.PUPIL_DELETED, from.id)
        }
    }

    override fun isAcceptable(data: String): Boolean = data.startsWith("/delete_user")
}