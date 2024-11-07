package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.update.abstracts.Update

private const val BOLD_MAKER = "*"

fun String?.makeBold() = this?.let { "$BOLD_MAKER$this$BOLD_MAKER" }

fun Int?.makeBold() = this.let { "$BOLD_MAKER$this$BOLD_MAKER" }

fun Update.userId(): Long = data.asFromUser()!!.from.id.chatId.long

fun CommonMessage<MessageContent>.userId(): Long = chat.id.chatId.long
