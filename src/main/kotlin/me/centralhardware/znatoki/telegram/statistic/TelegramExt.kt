package me.centralhardware.znatoki.telegram.statistic

import org.telegram.telegrambots.meta.api.objects.Update

private const val BOLD_MAKER = "*"
fun String?.makeBold() = this?.let { "$BOLD_MAKER$this$BOLD_MAKER" }
fun Int?.makeBold() = this.let { "$BOLD_MAKER$this$BOLD_MAKER" }

fun Update.userId(): Long = when{
    hasMessage() -> message.from.id
    hasCallbackQuery() -> callbackQuery.from.id
    hasInlineQuery() -> inlineQuery.from.id
    else -> {throw IllegalArgumentException()}
}