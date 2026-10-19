package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import arrow.core.left
import arrow.core.right
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.parseDate
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import kotlin.collections.contains

fun String.validateFio(): Either<String, StudentId> =
    if (db { StudentMapper.existsByFio(this) }) {
        Either.Right(this.split(" ")[0].toInt().toStudentId())
    } else {
        Either.Left("ФИО не найдено")
    }

fun String.validateTutor(): Either<String, String> =
    split(" ").firstOrNull()?.toLongOrNull()?.let {
        if (db { TutorMapper.findByIdOrNull(TutorId(it)) } != null) {
            Either.Right(this)
        } else {
            Either.Left("Репетитор с таким ID не найден")
        }
    } ?: Either.Left("Неверный формат.")

fun Int?.validateAmount(): Either<String, Unit> {
    return if (Amount.validate(this)) {
        return Either.Right(Unit)
    } else {
        Either.Left("Введенное значение должно быть больше нуля")
    }
}


fun ChatContentMessage<MessageContent>.validateText(): Either<String, String> =
    this.text?.takeIf { this.content is TextContent && StringUtils.isNotBlank(it) }
        ?.let { Either.Right(it) }
        ?: Either.Left("Введите текст")

fun ChatContentMessage<MessageContent>.validateTelephone(): Either<String, String> =
    this.text?.takeIf { PhoneNumber.validate(it) }
        ?.let { Either.Right(it) }
        ?: Either.Left("Введите номер телефона")

fun ChatContentMessage<MessageContent>.validateEnum(variants: List<String>): Either<String, String> =
    this.text?.takeIf { this.content is TextContent && it in variants }
        ?.let { Either.Right(it) }
        ?: Either.Left("Выберите вариант из кастомный клавиатуры")

fun ChatContentMessage<MessageContent>.validateDate(): Either<String, LocalDate> =
    this.text.parseDate()
        .fold(
            { Either.Right(it) },
            { Either.Left("Ошибка обработки даты необходимо ввести в формате: дд ММ гггг") },
        )

fun ChatContentMessage<MessageContent>.validateInt(): Either<String, Int> =
    this.text?.toIntOrNull()?.right() ?: "Введите число".left()


fun ChatContentMessage<MessageContent>.validatePhoto(): Either<String, Unit> =
    if (this.content is PhotoContent) {
        Either.Right(Unit)
    } else {
        Either.Left("Отправьте фото")
    }