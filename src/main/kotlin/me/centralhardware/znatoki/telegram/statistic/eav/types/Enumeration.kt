package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import org.telegram.telegrambots.meta.api.objects.Update

typealias EnumType = Enumeration
class Enumeration : Type {
    override fun format(name: String, isOptional: Boolean): String {
        return "Выберите $name. ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(update: Update, variants: List<String>): Either<String, Unit> {
        return if (update.hasMessage() && variants.contains(update.message.text)) {
            Either.Right(Unit)
        } else {
            Either.Left("Выберите вариант из кастомный клавиатуры")
        }
    }

}