package me.centralhardware.znatoki.telegram.statistic.telegram.conversation

import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.web.sendPaymentLog
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.telegram.BotAudit
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.KSLog
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.telegram.conversation.CANCEL
import me.centralhardware.telegram.conversation.ConversationCancelledException
import me.centralhardware.telegram.conversation.waitConfirmation
import me.centralhardware.telegram.conversation.waitEnum
import me.centralhardware.telegram.conversation.waitInlineSearch
import me.centralhardware.telegram.conversation.waitInt
import me.centralhardware.telegram.conversation.waitPhoto
import me.centralhardware.znatoki.telegram.statistic.extensions.extract
import me.centralhardware.znatoki.telegram.statistic.telegram.errorOrNull
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import me.centralhardware.znatoki.telegram.statistic.validateTutor
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyConfig
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.extensions.user

/**
 * Creates a new payment using wait-based conversation flow
 */
suspend fun BehaviourContext.createPayment(
    message: ChatContentMessage<MessageContent>,
    canAddForOthers: Boolean = false
) {
    val chatId = message.chat.id
    val builder = PaymentBuilder()

    var saved = false

    try {
        sendTextMessage(chatId, "Начало создания оплаты. Используйте $CANCEL для отмены в любой момент.")

        // Step 1: Select tutor (if allowed)
        val targetTutor = if (canAddForOthers) {
            val tutorText = waitInlineSearch(
                chatId = chatId,
                prompt = "Выберите репетитора",
                query = "t: ",
                buttonText = "Поиск",
                validate = { it.validateTutor().errorOrNull() }
            )
            val tutorId = TutorId(tutorText!!.split(" ")[0].toLong())
            builder.tutorId = tutorId
            TutorMapper.findByIdOrNull(tutorId) ?: data.user
        } else {
            data.user
        }

        // Step 2: Enter student FIO
        val fioText = waitInlineSearch(
            chatId = chatId,
            prompt = "Введите фио. \nнажмите для поиска фио",
            query = "s: ",
            buttonText = "Поиск",
            validate = { it.validateFio().errorOrNull() }
        )
        builder.studentId = fioText!!.split(" ")[0].toInt().toStudentId()

        // Step 3: Select subject
        val options = targetTutor.subjects.map { SubjectMapper.getNameById(it) }
        val subjectName = waitEnum(
            chatId = chatId,
            prompt = "Выберите предмет",
            options = options
        )
        builder.subjectId = SubjectMapper.getIdByName(subjectName!!)

        // Step 4: Enter payment amount
        val amount = waitInt(
            chatId = chatId,
            prompt = "Введите сумму оплаты",
            validate = { it.validateAmount().errorOrNull() }
        )
        builder.amount = amount!!.toAmount()

        // Step 5: Upload photo report
        builder.photoReport = waitPhoto(chatId, "Введите фото отчетности")!!.extract(bot)

        // Step 6: Confirm and save
        val confirmPrompt = """
            ФИО: ${StudentMapper.findById(builder.studentId!!).fio()}
            Оплата: ${builder.amount?.amount}
        """.trimIndent()

        val confirmed = waitConfirmation(chatId, confirmPrompt)
        
        if (confirmed) {
            val addedBy = message.tutorId()
            if (builder.tutorId == null) {
                builder.tutorId = addedBy
            }
            builder.addedByTutorId = if (addedBy != builder.tutorId) addedBy else null
            builder.dataSource = DataSource.BOT
            val payment = builder.build()
            val paymentId = PaymentMapper.insert(payment)
            BotAudit.payment(addedBy.id, "Создано", paymentId, payment, listOf(Change("сумма", null, "${payment.amount.amount} ₽")))
            saved = true
            sendPaymentLog(payment, paymentId, addedBy, DataSource.BOT)

            // Export to Firefly III if enabled
            if (FireflyConfig.enabled) {
                launch {
                    val updatedPayment = payment.copy(id = paymentId)
                    FireflyService.exportPayment(updatedPayment)
                        .onFailure { error ->
                            sendTextMessage(
                                chatId,
                                "⚠️ Оплата сохранена, но не удалось экспортировать в Firefly: ${error.message}"
                            )
                        }
                }
            }

            sendTextMessage(message.chat, "Сохранено", replyMarkup = ReplyKeyboardRemove())
        } else {
            MinioService.delete(builder.photoReport!!).onFailure {
                sendTextMessage(message.chat, "Ошибка при удаление фотографии")
            }
            sendTextMessage(message.chat, "Отменено", replyMarkup = ReplyKeyboardRemove())
        }
    } catch (e: ConversationCancelledException) {
        // Clean up photo if uploaded
        builder.photoReport?.let { key ->
            MinioService.delete(key).onFailure {
                sendTextMessage(chatId, "Ошибка при удалении фотографии")
            }
        }
        sendTextMessage(chatId, "Создание оплаты отменено", replyMarkup = ReplyKeyboardRemove())
    } catch (e: Exception) {
        // Clean up photo if uploaded and not referenced by a saved record
        builder.photoReport?.takeUnless { saved }?.let { key ->
            MinioService.delete(key).onFailure {
                sendTextMessage(chatId, "Ошибка при удалении фотографии")
            }
        }
        KSLog.error("createPayment failed", e)
        sendTextMessage(chatId, "Произошла ошибка, операция отменена", replyMarkup = ReplyKeyboardRemove())
    }
}

