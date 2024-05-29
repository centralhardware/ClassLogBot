package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT

typealias EnumType = Enumeration
data object Enumeration : Type {
    override fun format(name: String, isOptional: Boolean): String {
        return "Выберите $name. ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(message: CommonMessage<MessageContent>, variants: List<String>): Either<String, Unit> {
        return if (message.content is TextContent && variants.contains(message.text)) {
            Either.Right(Unit)
        } else {
            Either.Left("Выберите вариант из кастомный клавиатуры")
        }
    }

}