package me.centralhardware.znatoki.telegram.statistic.web

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService

/*
 * The log-chat messages for records entered in the web app. They match what the bot posts for its
 * own /addlesson and /addpayment (same text and the same buttons, which the bot's callbacks handle),
 * so the log chat looks the same whichever way a record was entered.
 */

private fun tutorLine(label: String, tutorId: TutorId, addedBy: TutorId?): String {
    val tutorName = TutorMapper.findByIdOrNull(tutorId)?.name?.hashtag()
    return if (addedBy != null && addedBy != tutorId) {
        "$label: $tutorName (внесено ${TutorMapper.findByIdOrNull(addedBy)?.name?.hashtag()})"
    } else {
        "$label: $tutorName"
    }
}

suspend fun TelegramBot.sendWebLessonLog(lessons: List<Lesson>, addedBy: TutorId?) {
    val lesson = lessons.first()
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "timeDelete-${lesson.id.id}") }
        row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${lesson.id.id}") }
        if (lessons.size == 1) row { dataButton("Сделать групповым занятием", "forceGroupAdd-${lesson.id.id}") }
    }
    val text = buildString {
        appendLine("#занятие")
        appendLine("Время: ${lesson.dateTime.formatDateTime()}")
        appendLine("Предмет: ${SubjectMapper.getNameById(lesson.subjectId).hashtag()}")
        appendLine("ученик: " + lessons.joinToString(", ") { "#${StudentMapper.getFioById(it.studentId).replace(" ", "_")}" })
        appendLine("Стоимость: ${lesson.amount}")
        append(tutorLine("Преподаватель", lesson.tutorId, addedBy))
    }
    val photo = MinioService.getBytes(lesson.photoReport!!).getOrThrow()
    sendPhoto(Config.logChat(), photo.asMultipartFile("Отчёт"), replyMarkup = keyboard, text = text)
}

suspend fun TelegramBot.sendWebPaymentLog(payment: Payment, id: PaymentId, addedBy: TutorId?) {
    val keyboard = inlineKeyboard { row { dataButton("Удалить", "paymentDelete-${id.id}") } }
    val text = buildString {
        appendLine("#оплата")
        appendLine("Время: ${payment.dateTime.formatDateTime()}")
        appendLine("Ученик: ${StudentMapper.findById(payment.studentId).fio().hashtag()}")
        appendLine("Предмет: ${SubjectMapper.getNameById(payment.subjectId)}")
        appendLine("Сумма: ${payment.amount.amount}")
        append(tutorLine("Оплату принял", payment.tutorId, addedBy))
    }
    val photo = MinioService.getBytes(payment.photoReport!!).getOrThrow()
    sendPhoto(Config.logChat(), photo.asMultipartFile("Отчёт"), replyMarkup = keyboard, text = text)
}
