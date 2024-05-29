package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.tgbotapi.extensions.utils.types.buttons.ReplyKeyboardRowBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.types.switchInlineQueryField
import dev.inmo.tgbotapi.utils.row

fun ReplyKeyboardRowBuilder.yes(
) = add(SimpleKeyboardButton("да"))

fun ReplyKeyboardRowBuilder.no(
) = add(SimpleKeyboardButton("нет"))

val switchToInlineKeyboard = inlineKeyboard {
    row { switchInlineQueryField }
}