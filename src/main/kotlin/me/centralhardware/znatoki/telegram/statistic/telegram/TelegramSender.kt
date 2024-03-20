package me.centralhardware.znatoki.telegram.statistic.telegram

import me.centralhardware.znatoki.telegram.statistic.i18n.ConstantEnum
import me.centralhardware.znatoki.telegram.statistic.limiter.Limiter
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import java.io.File
import java.util.*

@Component
class TelegramSender(
    private val limiter: Limiter,
    private val resourceBundle: ResourceBundle,
    private val telegramClient: OkHttpTelegramClient
) {

    companion object {
        private const val PARSE_MODE_MARKDOWN = "markdown"
    }

    fun sendText(text: String, chatId: Long) {
        sendText(text, chatId, true)
    }

    fun sendText(text: String, chatId: Long, removeKeyboard: Boolean) {
        val message = SendMessage.builder()
                .chatId(chatId)
                .text(text)

        if (removeKeyboard) {
            val removeMarkup = ReplyKeyboardRemove()
            removeMarkup.removeKeyboard = true
            message.replyMarkup(removeMarkup)
        }

        send {
            telegramClient.execute(message.build())
        }
    }

    fun send(block: OkHttpTelegramClient.() -> Unit) = limiter.limit{
        block(telegramClient)
    }

    private fun sendMessageFromResource(key: ConstantEnum, chatId: Long, deleteKeyboard: Boolean) {
        sendText(resourceBundle.getString(key.key()), chatId, deleteKeyboard)
    }

    fun sendMessageFromResource(key: ConstantEnum, chatId: Long) {
        sendMessageFromResource(key, chatId, true)
    }

    val removeKeyboard: ReplyKeyboardRemove = ReplyKeyboardRemove
        .builder()
        .removeKeyboard(true)
        .build()
    fun sendMessageAndRemoveKeyboard(text: String, chatId: Long) {
        send {
            execute(
                SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(removeKeyboard)
                    .build()
            )
        }
    }

    fun sendMessageWithMarkdown(text: String, chatId: Long) {
        send{
            execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(PARSE_MODE_MARKDOWN)
                .build())
        }
    }
}