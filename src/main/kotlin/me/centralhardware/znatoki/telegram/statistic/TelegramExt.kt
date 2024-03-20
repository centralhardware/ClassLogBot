package me.centralhardware.znatoki.telegram.statistic

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

private const val BOLD_MAKER = "*"
fun String?.makeBold() = this?.let { "$BOLD_MAKER$this$BOLD_MAKER" }
fun Int?.makeBold() = this.let { "$BOLD_MAKER$this$BOLD_MAKER" }

fun Update.from(): User = when{
    hasMessage() -> message.from
    hasCallbackQuery() -> callbackQuery.from
    hasInlineQuery() -> inlineQuery.from
    else -> {throw IllegalArgumentException()}
}

fun Update.userId(): Long = from().id