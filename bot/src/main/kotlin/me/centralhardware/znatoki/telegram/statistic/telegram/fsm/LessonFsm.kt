package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

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
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toAmount
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentIds
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.telegram.InlineSearchType
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import me.centralhardware.znatoki.telegram.statistic.validateTutor
import ru.nsk.kstatemachine.statemachine.StateMachine

suspend fun BehaviourContext.startLessonFsm(
    message: CommonMessage<MessageContent>,
    canAddForOthers: Boolean = false
): StateMachine {
    val builder = ServiceBuilder().apply {
        if (!canAddForOthers) {
            tutorId = message.tutorId()
        }
        if (data.user.subjects.size == 1) subjectId = data.user.subjects.first()
    }

    return startTelegramFsm(
        name = "time",
        ctx = builder,
        msg = message
    ) {
        if (canAddForOthers) {
            text(
                prompt = "Выберите репетитора",
                inline = true,
                inlineSearchType = InlineSearchType.TUTOR,
                optionalSkip = false,
                validator = { it.validateTutor() }
            ) { ctx, value ->
                ctx.tutorId = TutorId(value.split(" ")[0].toLong())
            }
        }

        if (data.user.subjects.size != 1) {
            val options = data.user.subjects.map { SubjectMapper.getNameById(it) }
            enum(
                prompt = "Выберите предмет",
                options = options
            ) { ctx, chosenName -> ctx.subjectId = SubjectMapper.getIdByName(chosenName) }
        }

        val groupAllowed = builder.subjectId?.let { SubjectMapper.isAllowGroup(it) } ?: false

        multi(
            prompt = if (groupAllowed) {
                "Введите ФИО по одному. Когда закончите — введите /complete"
            } else {
                "Введите ФИО."
            },
            true,
            maxCount = if (groupAllowed) Int.MAX_VALUE else 1,
            parse = { it.validateFio() }
        ) { builder, value ->
            builder.studentIds = value
        }

        int(
            prompt = "Введите стоимость занятия",
            validator = { it.validateAmount() }
        ) { ctx, value ->
            ctx.amount = value.toAmount()
        }

        photo(
            prompt = "Загрузите фото отчётности"
        ) { ctx, key -> ctx.photoReport = key }

        confirm(
            prompt = { ctx ->
                """
                предмет: ${SubjectMapper.getNameById(ctx.subjectId!!)}
                ФИО: ${ctx.studentIds.joinToString(";") { StudentMapper.getFioById(it) }}
                стоимость: ${ctx.amount?.amount}
                Сохранить?
                """.trimIndent()
            },
            onSave = { ctx ->
                ctx.id = LessonId.random()
                val addedBy = message.tutorId()
                ctx.addedByTutorId = if (addedBy != ctx.tutorId) addedBy else null
                val services = ctx.build()
                services.forEach { LessonMapper.insert(it) }
                sendLog(services, message.userId(), addedBy)
                
                val lesson = services.first()

                AuditLogMapper.log(
                    userId = addedBy.id,
                    action = "CREATE_LESSON",
                    entityType = "lesson",
                    entityId = null,
                    studentId = lesson.studentId.id,
                    subjectId = lesson.subjectId.id.toInt(),
                    null,
                    lesson
                )

                val allowForceGroup = data.user.hasForceGroup()
                val allowExtraHalf = data.user.hasExtraHalfHour()
                if ((allowForceGroup && services.size == 1) || allowExtraHalf) {
                    runBlocking {
                        sendTextMessage(
                            message.chat,
                            "Сохранено",
                            replyMarkup = inlineKeyboard {
                                if (allowForceGroup) row {
                                    dataButton(
                                        "Сделать групповым занятием",
                                        "forceGroupAdd-${ctx.id.id}"
                                    )
                                }
                                if (allowExtraHalf) row {
                                    dataButton(
                                        "Сделать полтора часа",
                                        "addExtraHalfHour-${ctx.id.id}"
                                    )
                                }
                            }
                        )
                        val tmp =
                            sendTextMessage(message.chat, "temp", replyMarkup = ReplyKeyboardRemove())
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
            },
            onCancel = { ctx ->
                ctx.photoReport?.let { key ->
                    MinioService.delete(key).onFailure {
                        runBlocking { sendTextMessage(ctx.tutorId!!.toChatId(), "Ошибка при удалении фотографии") }
                    }
                }
                sendTextMessage(
                    ctx.tutorId!!.toChatId(),
                    "Отменено",
                    replyMarkup = ReplyKeyboardRemove()
                )
            }
        )
    }
}

suspend fun BehaviourContext.sendLog(lessons: List<Lesson>, userId: Long, addedBy: TutorId? = null) {
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
