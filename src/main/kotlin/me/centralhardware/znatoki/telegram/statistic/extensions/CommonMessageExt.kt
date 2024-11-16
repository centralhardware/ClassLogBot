package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent

fun CommonMessage<MessageContent>.userId(): Long = chat.id.chatId.long