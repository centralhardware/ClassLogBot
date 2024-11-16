package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.types.update.abstracts.Update

fun Update.userId(): Long = data.asFromUser()!!.from.id.chatId.long