package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.parseDate
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
open class Date : Type {

    override fun format(name: String, isOptional: Boolean) =
        "Введите $name в формате dd MM yyyy ${if(isOptional) OPTIONAL_TEXT else ""}"

    override fun validate(update: Update, variants: List<String>): Either<String, Unit> =
        runCatching { update.message.text.parseDate() }
            .fold(
                { Either.Left("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy")},
                { Either.Right(Unit)}
            )
}