package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.parseDateTime
import org.telegram.telegrambots.meta.api.objects.Update

object DateTime : Type {

    override fun format(name: String, isOptional: Boolean): String {
        return "Введите $name в формате dd MM yyyy HH;mm ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(update: Update, variants: List<String>): Either<String, Unit> =
        update.message.text.parseDateTime()
            .fold(
                { Either.Right(Unit) },
                { Either.Left("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy") }
            )

}