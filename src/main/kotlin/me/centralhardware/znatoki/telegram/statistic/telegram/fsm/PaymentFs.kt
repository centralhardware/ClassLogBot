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
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio

private suspend fun BehaviourContext.sendLog(payment: Payment, paymentId: Int, userId: Long) {
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "paymentDelete-${paymentId}") }
    }
    val text =
        """
                #оплата
                Время: ${payment.dateTime.formatDateTime()}
                Клиент: ${StudentMapper.findById(payment.studentId)?.fio().hashtag()}
                Предмет: ${SubjectMapper.getNameById(payment.subjectId)}
                Оплата: ${payment.amount}
                Оплатил: ${data.user.name.hashtag()}
            """
            .trimIndent()
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

suspend fun BehaviourContext.startPaymentFsm(message: CommonMessage<MessageContent>) =
    startTelegramFsm(
        "paymentFsm",
        PaymentBuilder(),
        message
    ) {
        text(
            "Введите фио. \nнажмите для поиска фио",
            inline = true,
            optionalSkip = false,
            validator = ::validateFio
        ) { builder, value ->
            builder.studentId = value.split(" ")[0].toInt()
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
            ::validateAmount
        ) { builder, value ->
            builder.amount = Amount(value)
        }

        photo(
            "Введите фото отчетности"
        ) { builder, value -> builder.photoReport = value }

        confirm(
            {
                """
                                        ФИО: ${StudentMapper.findById(it.studentId!!)?.fio()}
                                        Оплата: ${it.amount}
                                        """
            },
            {
                it.tutorId = message.tutorId()
                val payment = it.build()
                val paymentId = PaymentMapper.insert(payment)
                sendLog(payment, paymentId, message.userId())
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
