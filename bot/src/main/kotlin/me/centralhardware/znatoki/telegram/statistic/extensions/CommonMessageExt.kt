package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.asPhotoContent
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import java.time.LocalDateTime

fun ChatContentMessage<MessageContent>.userId(): Long = chat.id.chatId.long
fun ChatContentMessage<MessageContent>.tutorId(): TutorId = TutorId(this.userId())

suspend fun ChatContentMessage<MessageContent>.extract(): String {
    val photo = this.content.asPhotoContent()!!.media
    val bytes = telegramBot(AppConfig.botToken()).downloadFile(photo)
    return MinioService.upload(bytes, LocalDateTime.now()).getOrThrow()
}

fun ChatContentMessage<MessageContent>.textOrNull(): String? = content.asTextContent()?.text