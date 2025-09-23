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
import me.centralhardware.znatoki.telegram.statistic.entity.toAmount
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentIds
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import ru.nsk.kstatemachine.statemachine.StateMachine

suspend fun BehaviourContext.startTimeFsm(message: CommonMessage<MessageContent>): StateMachine {
    val builder = ServiceBuilder().apply {
        tutorId = message.tutorId()
        if (data.user.subjects.size == 1) subjectId = data.user.subjects.first()
    }

    return startTelegramFsm(
        name = "time",
        ctx = builder,
        msg = message
    ) {
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
                val services = ctx.build()
                services.forEach { LessonMapper.insert(it) }
                sendLog(services, message.userId())

                val allowForceGroup = data.user.hasForceGroup()
                val allowExtraHalf = data.user.hasExtraHalfHour()
                if ((allowForceGroup && services.size == 1) || allowExtraHalf) {
                    runBlocking {
                        sendTextMessage(
                            ctx.tutorId!!.toChatId(),
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
                            sendTextMessage(ctx.tutorId!!.toChatId(), "temp", replyMarkup = ReplyKeyboardRemove())
                        deleteMessage(tmp.chat, tmp.messageId)
                    }
                } else {
                    runBlocking {
                        sendTextMessage(
                            ctx.tutorId!!.toChatId(),
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

suspend fun BehaviourContext.sendLog(lessons: List<Lesson>, userId: Long) {
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
        appendLine("Преподаватель: ${data.user.name.hashtag()}")
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
