package me.centralhardware.znatoki.telegram.statistic.telegram

import arrow.core.Either
import me.centralhardware.telegram.conversation.Parsed
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.extensions.parseDate
import java.time.LocalDate

/**
 * Bot-specific adapters that turn domain validation/parsing into the conversation library's
 * [Parsed] result. The generic wait-helpers live in `me.centralhardware.telegram.conversation`;
 * everything here depends on this bot's entities and date/phone formats.
 */

fun <T> Either<String, T>.toParsed(): Parsed<T> = fold({ Parsed.Err(it) }, { Parsed.Ok(it) })

/** Error string for the library `validate` callbacks (null when the input is acceptable). */
fun <T> Either<String, T>.errorOrNull(): String? = fold({ it }, { null })

fun parseDateInput(text: String): Parsed<LocalDate> =
    text.parseDate().fold(
        { Parsed.Ok(it) },
        { Parsed.Err("Ошибка обработки даты необходимо ввести в формате: дд ММ гггг") },
    )

fun parsePhone(text: String): Parsed<String> =
    if (PhoneNumber.validate(text)) Parsed.Ok(text) else Parsed.Err("Введите номер телефона")

fun parseFio(
    text: String,
    duplicateCheck: (Triple<String, String, String>) -> Boolean = { true },
): Parsed<Triple<String, String, String>> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size !in 2..3) {
        return Parsed.Err("ФИО требуется ввести в формате: фамилия имя [отчество]")
    }
    val fio = if (words.size == 3) Triple(words[0], words[1], words[2]) else Triple(words[0], words[1], "")
    return if (duplicateCheck(fio)) Parsed.Ok(fio) else Parsed.Err("Данное ФИО уже содержится в базе данных")
}
