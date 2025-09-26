package me.centralhardware.znatoki.telegram.statistic.extensions

import arrow.core.Either
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.asPhotoContent
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.io.path.writeBytes

fun CommonMessage<MessageContent>.userId(): Long = chat.id.chatId.long

fun CommonMessage<MessageContent>.validateText(): Either<String, String> =
    if (this.content is TextContent && StringUtils.isNotBlank(this.text)) {
        Either.Right(this.text!!)
    } else {
        Either.Left("Введите текст")
    }

fun CommonMessage<MessageContent>.validateTelephone(): Either<String, String> =
    if (this.text!!.validateTelephone()) {
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
            { Either.Left("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy") },
        )

fun CommonMessage<MessageContent>.validateInt(): Either<String, Int> =
    if (StringUtils.isNumeric(this.text)) Either.Right(this.text!!.toInt())
    else Either.Left("Введите число")

fun CommonMessage<MessageContent>.validatePhoto(): Either<String, Unit> =
    if (this.content is PhotoContent) {
        Either.Right(Unit)
    } else {
        Either.Left("Отправьте фото")
    }


suspend fun CommonMessage<MessageContent>.extract(): String {
    val photo = this.content.asPhotoContent()!!.media
    val temp = Files.createTempFile("tg", "sdf")
    temp.writeBytes(telegramBot(AppConfig.botToken()).downloadFile(photo))
    return MinioService.upload(temp.toFile(), LocalDateTime.now()).getOrThrow()
}

fun CommonMessage<MessageContent>.textOrNull(): String? = content.asTextContent()?.text