package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineQueryInCurrentChatButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.utils.row

val yesNoKeyboard = replyKeyboard {
    row { simpleButton("да") }
    row { simpleButton("нет") }
}

val switchToInlineKeyboard = inlineKeyboard { row { inlineQueryInCurrentChatButton("inline", "") } }
