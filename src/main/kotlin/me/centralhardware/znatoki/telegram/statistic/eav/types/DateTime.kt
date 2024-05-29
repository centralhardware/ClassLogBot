package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.parseDateTime

object DateTime : Type {

    override fun format(name: String, isOptional: Boolean): String {
        return "Введите $name в формате dd MM yyyy HH;mm ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(message: CommonMessage<MessageContent>, variants: List<String>): Either<String, Unit> =
        message.text.parseDateTime()
            .fold(
                { Either.Right(Unit) },
                { Either.Left("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy") }
            )

}