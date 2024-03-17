package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import kotlinx.serialization.Serializable
import org.telegram.telegrambots.meta.api.objects.Update

@Serializable
sealed interface Type {

    companion object{
        const val OPTIONAL_TEXT: String = "/skip для пропуска."
    }

    fun format(name: String, isOptional: Boolean): String
    fun validate(update: Update, variants: List<String>): Either<String, Unit>

    fun extract(update: Update): String? = update.message.text

    fun validate(update: Update, vararg variants: String): Either<String, Unit> {
        if (update.hasMessage() && update.message.text == "/skip") {
            return Either.Right(Unit)
        }

        return validate(update, variants.toList())
    }

    fun name(): String = this.javaClass.simpleName

}