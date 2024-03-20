package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.validateTelephone
import org.telegram.telegrambots.meta.api.objects.Update

object Telephone : Type {

    override fun format(name: String, isOptional: Boolean): String {
        return "Введите телефон $name. ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(update: Update, variants: List<String>): Either<String, Unit> {
        return if (update.message.text.validateTelephone()) {
            Either.Right(Unit)
        } else {
            Either.Left("Введите номер телефона")
        }
    }

}