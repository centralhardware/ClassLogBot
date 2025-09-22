package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

fun DataCallbackQuery.isDm() = from!!.id.chatId.long == message!!.chat.id.chatId.long
fun DataCallbackQuery.userId() = from.id.chatId.long