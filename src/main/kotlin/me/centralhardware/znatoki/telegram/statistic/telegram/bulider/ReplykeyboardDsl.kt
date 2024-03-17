package me.centralhardware.znatoki.telegram.statistic.telegram.bulider

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import kotlin.properties.Delegates

class ReplyKeyboardDsl {

    private lateinit var text: String
    private var chatId by Delegates.notNull<Long>()
    private val keyboard: MutableList<KeyboardRow> = mutableListOf()

    fun text(text: String) {
        this.text = text
    }

    fun chatId(chatId: Long) {
        this.chatId = chatId
    }

    fun row(initializer: ReplyRow.() -> Unit) {
        keyboard.add(ReplyRow().apply(initializer).row)
    }

    fun build(): SendMessage = SendMessage.builder()
        .chatId(chatId.toString())
        .text(text)
        .replyMarkup(
            ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build())
        .build()
}

fun replyKeyboard(initializer: ReplyKeyboardDsl.() -> Unit): ReplyKeyboardDsl {
    return ReplyKeyboardDsl().apply(initializer)
}

class ReplyRow {

    val row: KeyboardRow = KeyboardRow()

    fun btn(text: String) = row.add(text)

}