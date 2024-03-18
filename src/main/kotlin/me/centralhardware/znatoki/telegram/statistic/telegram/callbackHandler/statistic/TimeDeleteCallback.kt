package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.inlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import java.util.UUID

@Component
class TimeDeleteCallback(
    val serviceMapper: ServiceMapper,
    private val paymentMapper: PaymentMapper,
    sender: TelegramSender,
    telegramService: TelegramService, userMapper: UserMapper
) : CallbackHandler(sender, telegramService, userMapper) {

    override fun handle(callbackQuery: CallbackQuery, from: User, data: String) {
        if (!telegramService.isAdmin(from.id)) {
            sender.sendText("Доступ запрещен", from.id)
        }

        val id = UUID.fromString(data.replace("timeDelete-", ""))

        if (serviceMapper.getOrgId(id) != getTelegramUser(from)?.organizationId) {
            sender.sendText("Доступ запрещен", from.id)
            return
        }

        serviceMapper.setDeleted(id, true)
        paymentMapper.setDeleteByTimeId(id, true)

        val editMessageReplyMarkup = EditMessageReplyMarkup
            .builder()
            .messageId((callbackQuery.message as Message).messageId)
            .chatId(callbackQuery.message.chatId)
            .replyMarkup(inlineKeyboard {
                row { btn("восстановить", "timeRestore-$id") }
            }.buildReplyMarkup())
            .build()
        sender.send { execute(editMessageReplyMarkup) }
    }

    override fun isAcceptable(data: String) = data.startsWith("timeDelete-")
}