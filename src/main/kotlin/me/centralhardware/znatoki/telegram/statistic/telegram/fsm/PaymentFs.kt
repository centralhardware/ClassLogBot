package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.telegram.InlineSearchType
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio

private suspend fun BehaviourContext.sendLog(
    payment: Payment,
    paymentId: PaymentId,
    addedBy: TutorId? = null
) {
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "paymentDelete-${paymentId.id}") }
    }
    val text = buildString {
        appendLine("#оплата")
        appendLine("Время: ${payment.dateTime.formatDateTime()}")
        appendLine("Ученик: ${StudentMapper.findById(payment.studentId).fio().hashtag()}")
        appendLine("Предмет: ${SubjectMapper.getNameById(payment.subjectId)}")
        appendLine("Сумма: ${payment.amount.amount}")
        val tutorName = TutorMapper.findByIdOrNull(payment.tutorId)?.name?.hashtag()
        if (addedBy != null && addedBy != payment.tutorId) {
            val addedByName = TutorMapper.findByIdOrNull(addedBy)?.name?.hashtag()
            append("Оплату принял: $tutorName (внесено $addedByName)")
        } else {
            append("Оплату принял: $tutorName")
        }
    }.trimEnd()
    sendActionUploadPhoto(Config.logChat())
    sendPhoto(
        Config.logChat(),
        InputFile.fromInput("") {
            MinioService.get(payment.photoReport!!)
                .onFailure {
                    runBlocking {
                        sendTextMessage(
                            Config.logChat(),
                            "Ошибка во время отправки лога",
                        )
                    }
                }
                .getOrThrow()
        },
        replyMarkup = keyboard,
        text = text,
    )
}

suspend fun BehaviourContext.startPaymentFsm(
    message: CommonMessage<MessageContent>,
    canAddForOthers: Boolean = false
) =
    startTelegramFsm(
        "paymentFsm",
        PaymentBuilder(),
        message
    ) {
        if (canAddForOthers) {
            text(
                "Выберите репетитора",
                inline = true,
                inlineSearchType = InlineSearchType.TUTOR,
                optionalSkip = false,
                validator = { text ->
                    text.split(" ").firstOrNull()?.toLongOrNull()?.let {
                        if (TutorMapper.findByIdOrNull(TutorId(it)) != null) {
                            arrow.core.Either.Right(text)
                        } else {
                            arrow.core.Either.Left("Репетитор с таким ID не найден")
                        }
                    } ?: arrow.core.Either.Left("Неверный формат.")
                }
            ) { builder, value ->
                builder.tutorId = TutorId(value.split(" ")[0].toLong())
            }
        }

        text(
            "Введите фио. \nнажмите для поиска фио",
            inline = true,
            optionalSkip = false,
            validator = { it.validateFio() }
        ) { builder, value ->
            builder.studentId = value.split(" ")[0].toInt().toStudentId()
        }

        enum(
            "Выберите предмет",
            data.user.subjects.map { SubjectMapper.getNameById(it) }
        ) { builder, value ->
            builder.subjectId = SubjectMapper.getIdByName(value)
        }

        int(
            "Введите сумму оплаты",
            false,
            { it.validateAmount() }
        ) { builder, value ->
            builder.amount = value.toAmount()
        }

        photo(
            "Введите фото отчетности"
        ) { builder, value -> builder.photoReport = value }

        confirm(
            {
                """
                ФИО: ${StudentMapper.findById(it.studentId!!)?.fio()}
                Оплата: ${it.amount?.amount}
                """.trimIndent()
            },
            {
                val addedBy = message.tutorId()
                if (it.tutorId == null) {
                    it.tutorId = addedBy
                }
                val payment = it.build()
                sendLog(payment, PaymentMapper.insert(payment), addedBy)
                sendTextMessage(message.chat, "Сохранено", replyMarkup = ReplyKeyboardRemove())
            },
            {
                MinioService.delete(it.photoReport!!).onFailure {
                    sendTextMessage(message.chat, "Ошибка при удаление фотографии")
                }
                sendTextMessage(message.chat, "Отменено", replyMarkup = ReplyKeyboardRemove())
            }
        )
    }
