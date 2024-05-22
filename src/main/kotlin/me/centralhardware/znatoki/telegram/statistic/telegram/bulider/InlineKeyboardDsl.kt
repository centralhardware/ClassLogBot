package me.centralhardware.znatoki.telegram.statistic.telegram.bulider

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import kotlin.properties.Delegates

class InlineKeyboardDsl {

    private lateinit var text: String
    private var chatId by Delegates.notNull<Long>()
    private val keyboard: MutableList<InlineKeyboardRow> = mutableListOf()

    fun text(text: String) {
        this.text = text
    }

    fun chatId(chatId: Long) {
        this.chatId = chatId
    }

    fun row(initializer: InlineRow.() -> Unit) {
        keyboard.add(InlineRow().apply(initializer).row)
    }

    fun build(): SendMessage = SendMessage.builder()
        .text(text)
        .chatId(chatId)
        .replyMarkup(buildReplyMarkup())
        .build()

    fun buildReplyMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup
        .builder()
        .keyboard(keyboard).build()
}

fun inlineKeyboard(initializer: InlineKeyboardDsl.() -> Unit): InlineKeyboardDsl {
    return InlineKeyboardDsl().apply(initializer)
}

class InlineRow {

    internal val row: InlineKeyboardRow = InlineKeyboardRow()

    fun btn(text: String, callbackData: String) {
        row.add(InlineKeyboardButton.builder().text(text).callbackData(callbackData).build())
    }

    fun switchToInline() {
        row.add(InlineKeyboardButton.builder().text("inline").switchInlineQueryCurrentChat("").build())
    }

    fun webApp(url: String, text: String) {
        row.add(
            InlineKeyboardButton
                .builder()
                .text(text)
                .webApp(
                    WebAppInfo
                        .builder()
                        .url(url)
                        .build()
                )
                .build()
        )
    }

}
