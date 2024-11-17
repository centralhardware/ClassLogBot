package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.asPhotoContent
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.io.path.writeBytes

data object Photo : Type {
    override fun format(name: String, isOptional: Boolean): String {
        return "Отправьте фото $name. ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(
        message: CommonMessage<MessageContent>,
        variants: List<String>,
    ): Either<String, Unit> {
        return if (message.content is PhotoContent) {
            Either.Right(Unit)
        } else {
            Either.Left("Отправьте фото")
        }
    }

    override suspend fun extract(message: CommonMessage<MessageContent>): String {
        val photo = message.content.asPhotoContent()!!.media
        val temp = Files.createTempFile("tg", "sdf")
        temp.writeBytes(telegramBot(AppConfig.botToken()).downloadFile(photo))
        return MinioService.upload(temp.toFile(), LocalDateTime.now()).getOrThrow()
    }
}
