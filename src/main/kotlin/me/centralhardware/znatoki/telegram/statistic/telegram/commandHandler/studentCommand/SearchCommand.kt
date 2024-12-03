package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.user
import org.apache.commons.collections4.CollectionUtils

suspend fun BehaviourContext.searchCommand() = onCommandWithArgs("s") { message, args ->
    val searchText = args.joinToString(separator = " ")
    val searchResult = ClientService.search(searchText)

    Trace.save("searchUser", mapOf("query" to searchText))
    if (CollectionUtils.isEmpty(searchResult)) {
        sendMessage(message.chat, "Ничего не найдено")
        return@onCommandWithArgs
    }

    sendMessage(message.chat, "результаты поиска")
    searchResult.forEach { client ->
        Trace.save("findUser", mapOf("query" to searchText, "userId" to client.id.toString()))
        sendMessage(
            message.chat,
            "${client.name} ${client.secondName} ${client.lastName}",
            replyMarkup =
                inlineKeyboard {
                    row { dataButton("информация", "user_info${client.id}") }
                    if (data.user.hasAdminPermission()) {
                        row { dataButton("удалить", "delete_user${client.id}") }
                    }
                },
        )
    }
}
