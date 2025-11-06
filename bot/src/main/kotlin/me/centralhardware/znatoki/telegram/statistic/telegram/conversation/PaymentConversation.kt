package me.centralhardware.znatoki.telegram.statistic.telegram.conversation

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
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.telegram.*
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import me.centralhardware.znatoki.telegram.statistic.validateTutor

/**
 * Creates a new payment using wait-based conversation flow
 */
suspend fun BehaviourContext.createPayment(
    message: CommonMessage<MessageContent>,
    canAddForOthers: Boolean = false
) {
    val chatId = message.chat.id
    val userId = message.userId()
    val builder = PaymentBuilder()
    
    try {
        sendTextMessage(chatId, "Начало создания оплаты. Используйте $CANCEL для отмены в любой момент.")
        
        // Step 1: Select tutor (if allowed)
        if (canAddForOthers) {
            val tutorText = waitValidatedText(
                chatId = chatId,
                userId = userId,
                prompt = "Выберите репетитора",
                useInline = true,
                inlineSearchType = InlineSearchType.TUTOR,
                validator = { text -> text.validateTutor().map { } }
            )
            builder.tutorId = TutorId(tutorText!!.split(" ")[0].toLong())
        }
        
        // Step 2: Enter student FIO
        val fioText = waitValidatedText(
            chatId = chatId,
            userId = userId,
            prompt = "Введите фио. \nнажмите для поиска фио",
            useInline = true,
            validator = { text -> text.validateFio().map { } }
        )
        builder.studentId = fioText!!.split(" ")[0].toInt().toStudentId()
        
        // Step 3: Select subject
        val options = data.user.subjects.map { SubjectMapper.getNameById(it) }
        val subjectName = waitEnum(
            chatId = chatId,
            userId = userId,
            prompt = "Выберите предмет",
            options = options
        )
        builder.subjectId = SubjectMapper.getIdByName(subjectName!!)
        
        // Step 4: Enter payment amount
        val amount = waitValidatedInt(
            chatId = chatId,
            userId = userId,
            prompt = "Введите сумму оплаты",
            validator = { it.validateAmount() }
        )
        builder.amount = amount!!.toAmount()
        
        // Step 5: Upload photo report
        val photoKey = waitValidatedPhoto(
            chatId = chatId,
            userId = userId,
            prompt = "Введите фото отчетности"
        )
        builder.photoReport = photoKey
        
        // Step 6: Confirm and save
        val confirmPrompt = """
            ФИО: ${StudentMapper.findById(builder.studentId!!)?.fio()}
            Оплата: ${builder.amount?.amount}
        """.trimIndent()
        
        val confirmed = waitConfirmation(chatId, userId, confirmPrompt)
        
        if (confirmed) {
            val addedBy = message.tutorId()
            if (builder.tutorId == null) {
                builder.tutorId = addedBy
            }
            builder.addedByTutorId = if (addedBy != builder.tutorId) addedBy else null
            builder.dataSource = DataSource.BOT
            val payment = builder.build()
            val paymentId = PaymentMapper.insert(payment)
            sendLog(payment, paymentId, addedBy)
            
            AuditLogMapper.log(
                userId = addedBy.id,
                action = "CREATE_PAYMENT",
                entityType = "payment",
                entityId = paymentId.id.toString(),
                studentId = payment.studentId.id,
                subjectId = payment.subjectId.id.toInt(),
                null,
                payment
            )
            
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
                runBlocking { sendTextMessage(chatId, "Ошибка при удалении фотографии") }
            }
        }
        sendTextMessage(chatId, "Создание оплаты отменено", replyMarkup = ReplyKeyboardRemove())
    } catch (e: Exception) {
        // Clean up photo if uploaded
        builder.photoReport?.let { key ->
            MinioService.delete(key).onFailure {
                runBlocking { sendTextMessage(chatId, "Ошибка при удалении фотографии") }
            }
        }
        sendTextMessage(chatId, "Операция отменена: ${e.message}", replyMarkup = ReplyKeyboardRemove())
    }
}

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
