package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import me.centralhardware.znatoki.telegram.statistic.user
import org.apache.commons.collections4.CollectionUtils

fun BehaviourContext.searchStudentCommand() = onCommandWithArgs("s") { message, args ->
    val searchText = args.joinToString(separator = " ")
    val searchResult = StudentService.search(searchText)

    if (CollectionUtils.isEmpty(searchResult)) {
        sendMessage(message.chat, "Ничего не найдено")
        return@onCommandWithArgs
    }

    sendMessage(message.chat, "результаты поиска")
    searchResult.forEach { client ->
        sendMessage(
            message.chat,
            "${client.name} ${client.secondName} ${client.lastName}",
            replyMarkup =
                inlineKeyboard {
                    row { dataButton("информация", "user_info${client.id?.id}") }
                    if (data.user.hasAdminPermission()) {
                        row { dataButton("удалить", "delete_user${client.id?.id}") }
                    }
                },
        )
    }
}
