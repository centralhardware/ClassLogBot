package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.parseDate

object Date : Type {

    override fun format(name: String, isOptional: Boolean) =
        "Введите $name в формате dd MM yyyy ${if(isOptional) OPTIONAL_TEXT else ""}"

    override fun validate(message: CommonMessage<MessageContent>, variants: List<String>): Either<String, Unit> =
        runCatching { message.text.parseDate() }
            .fold(
                { Either.Right(Unit)},
                { Either.Left("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy")}
            )
}