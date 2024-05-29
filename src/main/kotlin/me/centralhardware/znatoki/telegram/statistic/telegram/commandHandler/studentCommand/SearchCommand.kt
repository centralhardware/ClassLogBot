package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.i18n.load
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.userId
import org.apache.commons.collections4.CollectionUtils
import java.util.stream.Collectors

suspend fun searchCommand(message: CommonMessage<TextContent>, args: Array<String>) {
    val orgId = UserMapper.findById(message.userId())!!.organizationId
    val searchText = args.joinToString(separator = " ")
    val searchResult = ClientService.search(searchText)
        .stream()
        .filter { it.organizationId == orgId }
        .collect(Collectors.toList())

    if (CollectionUtils.isEmpty(searchResult)) {
        bot.sendMessage(message.chat, I18n.Message.NOTHING_FOUND.load())
        return
    }

    bot.sendMessage(message.chat, I18n.Message.SEARCH_RESULT.load())
    searchResult.forEach { client ->
        bot.sendMessage(message.chat,
            "${client.name} ${client.secondName} ${client.lastName}",
            replyMarkup = inlineKeyboard{
                row { dataButton("информация", "/user_info${client.id}") }
                if (UserMapper.hasWriteRight(message.userId())){
                    row { dataButton("удалить", "/delete_user${client.id}") }
                }
            })
    }
}