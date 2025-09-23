package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.parseDate
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import kotlin.collections.contains

fun String.validateFio(): Either<String, StudentId> =
    if (StudentMapper.existsByFio(this)) {
        Either.Right(this.split(" ")[0].toInt().toStudentId())
    } else {
        Either.Left("ФИО не найдено")
    }

fun Int?.validateAmount(): Either<String, Unit> {
    return if (Amount.validate(this)) {
        return Either.Right(Unit)
    } else {
        Either.Left("Введенное значение должно быть больше нуля")
    }
}


fun CommonMessage<MessageContent>.validateText(): Either<String, String> =
    if (this.content is TextContent && StringUtils.isNotBlank(this.text)) {
        Either.Right(this.text!!)
    } else {
        Either.Left("Введите текст")
    }

fun CommonMessage<MessageContent>.validateTelephone(): Either<String, String> =
    if (PhoneNumber.validate(this.text!!)) {
        Either.Right(this.text!!)
    } else {
        Either.Left("Введите номер телефона")
    }

fun CommonMessage<MessageContent>.validateEnum(variants: List<String>): Either<String, String> =
    if (this.content is TextContent && variants.contains(this.text)) {
        Either.Right(this.text!!)
    } else {
        Either.Left("Выберите вариант из кастомный клавиатуры")
    }

fun CommonMessage<MessageContent>.validateDate(): Either<String, LocalDate> =
    this.text.parseDate()
        .fold(
            { Either.Right(it) },
            { Either.Left("Ошибка обработки даты необходимо ввести в формате: дд ММ гггг") },
        )

fun CommonMessage<MessageContent>.validateInt(): Either<String, Int> =
    this.text?.toIntOrNull()?.right() ?: "Введите число".left()


fun CommonMessage<MessageContent>.validatePhoto(): Either<String, Unit> =
    if (this.content is PhotoContent) {
        Either.Right(Unit)
    } else {
        Either.Left("Отправьте фото")
    }