package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDeepLink
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import me.centralhardware.znatoki.telegram.statistic.web.BotLogin
import me.centralhardware.znatoki.telegram.statistic.web.TelegramUser

fun BehaviourContext.startCommand() {
    onCommand("start") {
        sendMessage(
            it.chat,
            """
            Бот для управления учениками, занятиями и оплатами.
            Автор: @centralhardware
            """.trimIndent(),
        )
    }

    // /start login_<token>: the sign-in screen of the app outside Telegram sent the user here.
    onDeepLink(Regex("${BotLogin.PREFIX}\\w+")) { (message, link) ->
        val user = (message as? FromUserMessage)?.from ?: return@onDeepLink
        val ok = BotLogin.confirm(
            link.removePrefix(BotLogin.PREFIX),
            TelegramUser(user.id.chatId.long, user.firstName, user.lastName.takeIf { it.isNotEmpty() }, user.username?.withoutAt),
        )
        sendMessage(message.chat, if (ok) "Вход выполнен, вернитесь в приложение." else "Ссылка для входа устарела, начните вход в приложении заново.")
    }
}
