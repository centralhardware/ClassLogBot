package me.centralhardware.znatoki.telegram.statistic.telegram.conversation

import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
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
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.telegram.*
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import me.centralhardware.znatoki.telegram.statistic.validateTutor

/**
 * Creates a new lesson using wait-based conversation flow
 */
suspend fun BehaviourContext.createLesson(
    message: CommonMessage<MessageContent>,
    canAddForOthers: Boolean = false
) {
    val chatId = message.chat.id
    val userId = message.userId()
    val builder = ServiceBuilder()
    
    try {
        sendTextMessage(chatId, "Начало создания занятия. Используйте $CANCEL для отмены в любой момент.")
        
        // Step 1: Select tutor (if allowed)
        val targetTutor = if (canAddForOthers) {
            val tutorText = waitValidatedText(
                chatId = chatId,
                userId = userId,
                prompt = "Выберите репетитора",
                useInline = true,
                inlineSearchType = InlineSearchType.TUTOR,
                validator = { text -> text.validateTutor().map { } }
            )
            val tutorId = TutorId(tutorText!!.split(" ")[0].toLong())
            builder.tutorId = tutorId
            TutorMapper.findByIdOrNull(tutorId) ?: data.user
        } else {
            builder.tutorId = message.tutorId()
            data.user
        }
        
        // Step 2: Select subject (if user has multiple subjects)
        if (targetTutor.subjects.size == 1) {
            builder.subjectId = targetTutor.subjects.first()
        } else {
            val options = targetTutor.subjects.map { SubjectMapper.getNameById(it) }
            val subjectName = waitEnum(
                chatId = chatId,
                userId = userId,
                prompt = "Выберите предмет",
                options = options
            )
            builder.subjectId = SubjectMapper.getIdByName(subjectName!!)
        }
        
        // Step 3: Collect student FIOs
        val groupAllowed = builder.subjectId?.let { SubjectMapper.isAllowGroup(it) } ?: false
        
        val students = if (groupAllowed) {
            waitMultiple(
                chatId = chatId,
                userId = userId,
                prompt = "Введите ФИО по одному. Когда закончите — введите $COMPLETE",
                useInline = true,
                parse = { text -> text.validateFio() }
            )
        } else {
            val fioText = waitValidatedText(
                chatId = chatId,
                userId = userId,
                prompt = "Введите ФИО.",
                useInline = true,
                validator = { text -> text.validateFio().map { } }
            )
            setOf(fioText!!.split(" ")[0].toInt().toStudentId())
        }
        builder.studentIds = students
        
        // Step 4: Enter lesson cost
        val amount = waitValidatedInt(
            chatId = chatId,
            userId = userId,
            prompt = "Введите стоимость занятия",
            validator = { it.validateAmount() }
        )
        builder.amount = amount!!.toAmount()
        
        // Step 5: Upload photo report
        val photoKey = waitValidatedPhoto(
            chatId = chatId,
            userId = userId,
            prompt = "Загрузите фото отчётности"
        )
        builder.photoReport = photoKey
        
        // Step 6: Confirm and save
        val confirmPrompt = """
            предмет: ${SubjectMapper.getNameById(builder.subjectId!!)}
            ФИО: ${builder.studentIds.joinToString(";") { StudentMapper.getFioById(it) }}
            стоимость: ${builder.amount?.amount}
            Сохранить?
        """.trimIndent()
        
        val confirmed = waitConfirmation(chatId, userId, confirmPrompt)
        
        if (confirmed) {
            builder.id = LessonId.random()
            val addedBy = message.tutorId()
            builder.addedByTutorId = if (addedBy != builder.tutorId) addedBy else null
            builder.dataSource = DataSource.BOT
            val services = builder.build()
            services.forEach { LessonMapper.insert(it) }
            sendLog(services, message.userId(), addedBy)
            
            val lesson = services.first()
            
            AuditLogMapper.log(
                userId = addedBy.id,
                action = "CREATE_LESSON",
                entityType = "lesson",
                entityId = lesson.id.id.toString(),
                studentId = lesson.studentId.id,
                subjectId = lesson.subjectId.id.toInt(),
                null,
                lesson
            )
            
            val allowForceGroup = targetTutor.hasForceGroup()
            val allowExtraHalf = targetTutor.hasExtraHalfHour()
            if ((allowForceGroup && services.size == 1) || allowExtraHalf) {
                runBlocking {
                    sendTextMessage(
                        message.chat,
                        "Сохранено",
                        replyMarkup = inlineKeyboard {
                            if (allowForceGroup) row {
                                dataButton(
                                    "Сделать групповым занятием",
                                    "forceGroupAdd-${builder.id.id}"
                                )
                            }
                            if (allowExtraHalf) row {
                                dataButton(
                                    "Сделать полтора часа",
                                    "addExtraHalfHour-${builder.id.id}"
                                )
                            }
                        }
                    )
                    val tmp = sendTextMessage(message.chat, "temp", replyMarkup = ReplyKeyboardRemove())
                    deleteMessage(tmp.chat, tmp.messageId)
                }
            } else {
                runBlocking {
                    sendTextMessage(
                        message.chat,
                        "Сохранено",
                        replyMarkup = ReplyKeyboardRemove()
                    )
                }
            }
        } else {
            builder.photoReport?.let { key ->
                MinioService.delete(key).onFailure {
                    runBlocking { sendTextMessage(builder.tutorId!!.toChatId(), "Ошибка при удалении фотографии") }
                }
            }
            sendTextMessage(
                builder.tutorId!!.toChatId(),
                "Отменено",
                replyMarkup = ReplyKeyboardRemove()
            )
        }
    } catch (e: ConversationCancelledException) {
        // Clean up photo if uploaded
        builder.photoReport?.let { key ->
            MinioService.delete(key).onFailure {
                runBlocking { sendTextMessage(chatId, "Ошибка при удалении фотографии") }
            }
        }
        sendTextMessage(chatId, "Создание занятия отменено", replyMarkup = ReplyKeyboardRemove())
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

private suspend fun BehaviourContext.sendLog(lessons: List<Lesson>, userId: Long, addedBy: TutorId? = null) {
    val service = lessons.first()
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "timeDelete-${service.id.id}") }
        row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${service.id.id}") }
        if (lessons.size == 1) row { dataButton("Сделать групповым занятием", "forceGroupAdd-${service.id.id}") }
    }

    val log = buildString {
        appendLine("#занятие")
        appendLine("Время: ${service.dateTime.formatDateTime()}")
        appendLine("Предмет: ${SubjectMapper.getNameById(service.subjectId).hashtag()}")
        appendLine(
            "ученик: " + lessons.toStudentIds()
                .joinToString(", ") { "#${StudentMapper.getFioById(it).replace(" ", "_")}" })
        appendLine("Стоимость: ${service.amount}")
        val tutorName = TutorMapper.findByIdOrNull(service.tutorId)?.name?.hashtag()
        if (addedBy != null && addedBy != service.tutorId) {
            val addedByName = TutorMapper.findByIdOrNull(addedBy)?.name?.hashtag()
            append("Преподаватель: $tutorName (внесено $addedByName)")
        } else {
            append("Преподаватель: $tutorName")
        }
    }.trimIndent()

    sendPhoto(
        Config.logChat(),
        InputFile.fromInput("Отчёт") {
            MinioService.get(service.photoReport!!)
                .onFailure { runBlocking { sendTextMessage(Config.logChat(), "Ошибка во время отправки лога") } }
                .getOrThrow()
        },
        replyMarkup = keyboard,
        text = log
    )
}
