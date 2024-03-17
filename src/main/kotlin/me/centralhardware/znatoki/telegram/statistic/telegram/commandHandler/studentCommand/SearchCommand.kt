package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.inlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.userId
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.stream.Collectors

@Component
class SearchCommand(
    private val clientService: ClientService,
    private val telegramService: TelegramService,
    sender: TelegramSender,
    userMapper: UserMapper,
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        val arguments = update.message.text.replace("/s", "").trim().split(" ").toTypedArray()
        if (arguments.size == 1 && StringUtils.isBlank(arguments[0])) {
            sender.sendText("Вы не ввели текст запроса. Пример: /s Иванов", update.userId())
            return
        }

        val orgId = userMapper.getById(update.userId())!!.organizationId
        val searchText = arguments.joinToString(separator = " ")
        val searchResult = clientService.search(searchText)
            .stream()
            .filter { !it.deleted }
            .filter { it.organizationId == orgId }
            .collect(Collectors.toList())

        if (CollectionUtils.isEmpty(searchResult)) {
            sender.sendMessageFromResource(I18n.Message.NOTHING_FOUND, update.userId())
            return
        }

        sender.sendMessageFromResource(I18n.Message.SEARCH_RESULT, update.userId())
        for (client in searchResult) {
            sender.send(inlineKeyboard {
                text("${client.name} ${client.secondName} ${client.lastName} \n")
                chatId(update.userId())
                row { btn("информация", "/user_info${client.id}") }
                if (telegramService.hasWriteRight(update.userId())) {
                    row { btn("удалить", "/delete_user${client.id}") }
                }
            })
        }
    }

    override fun isAcceptable(data: String): Boolean = data.startsWith("/s ")

    override fun getRequiredRole(): Role = Role.READ
}