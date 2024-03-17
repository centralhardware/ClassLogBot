package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User

@Component
class UserInfoCallback(
    val clientService: ClientService,
    val serviceMapper: ServiceMapper,
    val servicesMapper: ServicesMapper,
    sender: TelegramSender,
    telegramService: TelegramService,
    userMapper: UserMapper
) : CallbackHandler(sender, telegramService, userMapper) {

    override fun handle(callbackQuery: CallbackQuery, from: User, data: String) {
        if (!telegramService.hasReadRight(from.id)) {
            sender.sendMessageFromResource(I18n.Error.ACCESS_DENIED, from.id)
            return
        }
        clientService.findById(data.replace("/user_info", "").toInt())?.let { client ->
            if (client.organizationId != getTelegramUser(from)?.organizationId) {
                sender.sendText("Доступ запрещен", from.id)
                return
            }

            val info = client.getInfo(
                serviceMapper.getServicesForCLient(client.id!!).mapNotNull { servicesMapper.getNameById(it) }.toList()
            )
            sender.sendMessageWithMarkdown(info, from.id)
        } ?: let { sender.sendMessageFromResource(I18n.Message.USER_NOT_FOUND, from.id) }
    }

    override fun isAcceptable(data: String): Boolean {
        return data.startsWith("/user_info")
    }
}