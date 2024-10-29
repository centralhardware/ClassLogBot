package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.serialization.Serializable

@Serializable
sealed interface Type {

    companion object {
        const val OPTIONAL_TEXT: String = "/skip для пропуска."
    }

    fun format(name: String, isOptional: Boolean): String

    fun validate(
        message: CommonMessage<MessageContent>,
        variants: List<String>,
    ): Either<String, Unit>

    fun extract(message: CommonMessage<MessageContent>): String? = message.text

    fun validate(
        message: CommonMessage<MessageContent>,
        vararg variants: String,
    ): Either<String, Unit> {
        if (message.content is TextContent && message.text == "/skip") {
            return Either.Right(Unit)
        }

        return validate(message, variants.toList())
    }

    fun name(): String = this.javaClass.simpleName
}

fun String.toType() =
    when (this) {
        "Date" -> Date
        "DateTime" -> DateTime
        "Enumeration" -> Enumeration
        "Integer" -> Integer
        "Photo" -> Photo
        "Telephone" -> Telephone
        "Text" -> Text
        else -> throw IllegalArgumentException()
    }
