package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.asPhotoContent
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.io.path.writeBytes

fun CommonMessage<MessageContent>.userId(): Long = chat.id.chatId.long
fun CommonMessage<MessageContent>.tutorId(): TutorId = TutorId(this.userId())

suspend fun CommonMessage<MessageContent>.extract(): String {
    val photo = this.content.asPhotoContent()!!.media
    val temp = Files.createTempFile("tg", "sdf")
    temp.writeBytes(telegramBot(AppConfig.botToken()).downloadFile(photo))
    return MinioService.upload(temp.toFile(), LocalDateTime.now()).getOrThrow()
}

fun CommonMessage<MessageContent>.textOrNull(): String? = content.asTextContent()?.text