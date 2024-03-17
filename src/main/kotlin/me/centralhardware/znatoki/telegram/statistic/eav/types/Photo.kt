package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.utils.BeanUtils
import me.centralhardware.znatoki.telegram.statistic.utils.minioService
import me.centralhardware.znatoki.telegram.statistic.utils.telegramClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.LocalDateTime

open class Photo : Type {
    override fun format(name: String, isOptional: Boolean): String {
        return "Отправьте фото $name. ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(update: Update, variants: List<String>): Either<String, Unit> {
        return if (update.hasMessage() && update.message.hasPhoto()) {
            Either.Right(Unit)
        } else{
            Either.Left("Отправьте фото")
        }
    }

    override fun extract(update: Update): String? {
        val photo = update.message?.photo?.maxBy { it.fileSize }
        val file = telegramClient().downloadFile(telegramClient().execute(photo?.let { GetFile.builder().fileId(it.fileId).build() }))
        return minioService().upload(file, LocalDateTime.now()).getOrNull()
    }
}