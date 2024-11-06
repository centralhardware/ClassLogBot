package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.userId
import org.apache.commons.collections4.CollectionUtils

suspend fun searchCommand(message: CommonMessage<TextContent>, args: Array<String>) {
    val searchText = args.joinToString(separator = " ")
    val searchResult = ClientService.search(searchText)

    Trace.save("searchUser", mapOf("query" to searchText))
    if (CollectionUtils.isEmpty(searchResult)) {
        bot.sendMessage(message.chat, "Ничего не найдено")
        return
    }

    bot.sendMessage(message.chat, "результаты поиска")
    searchResult.forEach { client ->
        Trace.save(
            "findUser",
            mapOf("query" to searchText, "userId" to client.id.toString()),
        )
        bot.sendMessage(
            message.chat,
            "${client.name} ${client.secondName} ${client.lastName}",
            replyMarkup =
                inlineKeyboard {
                    row { dataButton("информация", "user_info${client.id}") }
                    if (UserMapper.hasAdminPermission(message.userId())) {
                        row { dataButton("удалить", "delete_user${client.id}") }
                    }
                },
        )
    }
}
