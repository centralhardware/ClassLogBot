package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import org.apache.commons.lang3.StringUtils
import org.telegram.telegrambots.meta.api.objects.Update

class Text: Type {
    override fun format(name: String, isOptional: Boolean): String {
        return "Введите $name. ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(update: Update, variants: List<String>): Either<String, Unit> {
        return if (update.hasMessage() && StringUtils.isNotBlank(update.message.text)) {
            Either.Right(Unit)
        } else {
            Either.Left("Введите текст")
        }
    }

}