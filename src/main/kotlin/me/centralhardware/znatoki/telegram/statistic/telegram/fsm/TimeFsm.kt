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
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import ru.nsk.kstatemachine.statemachine.StateMachine
import java.util.*

suspend fun BehaviourContext.startTimeFsm(message: CommonMessage<MessageContent>): StateMachine {
    val userId = message.userId()
    val user = UserMapper.findById(userId) ?: error("User not found: $userId")

    val builder = ServiceBuilder().apply {
        chatId = userId
        if (user.services.size == 1) serviceId = user.services.first()
    }

    return startTelegramFsm(
        name = "time",
        ctx = builder,
        msg = message
    ) {
        if (user.services.size != 1) {
            val options = user.services.mapNotNull { ServicesMapper.getNameById(it) }
            enum(
                prompt = "Выберите предмет",
                options = options
            ) { ctx, chosenName -> ctx.serviceId = ServicesMapper.getServiceId(chosenName) }
        }

        val groupAllowed = builder.serviceId?.let { ServicesMapper.isAllowMultiplyClients(it) } ?: false

        multi(
            prompt = if (groupAllowed) {
                "Введите ФИО по одному. Когда закончите — введите /complete"
            } else {
                "Введите ФИО."
            },
            true,
            maxCount = if (groupAllowed) Int.MAX_VALUE else 1,
            parse = { validateFio(it) }
        ) { builder, value ->
            builder.clientIds = value
        }

        int(
            prompt = "Введите стоимость занятия",
            validator = {
                validateAmount(it)
            }
        ) { ctx, value ->
            ctx.amount = value
            runBlocking {
                sendTextMessage(
                    ctx.chatId!!.toChatId(),
                    "Пришлите фото отчётности",
                    replyMarkup = ReplyKeyboardRemove()
                )
            }
        }

        photo(
            prompt = "Загрузите фото отчётности"
        ) { ctx, key -> ctx.photoReport = key }

        confirm(
            prompt = { ctx ->
                """
                услуга: ${ServicesMapper.getNameById(ctx.serviceId!!)}
                ФИО: ${ctx.clientIds.joinToString(";") { ClientMapper.getFioById(it) }}
                стоимость: ${ctx.amount}
                Сохранить?
                """.trimIndent()
            },
            onSave = { ctx ->
                ctx.id = UUID.randomUUID()
                val services = ctx.build()
                services.forEach { ServiceMapper.insert(it) }
                sendLog(services, userId)

                val allowForceGroup = UserMapper.findById(userId)!!.hasForceGroup()
                val allowExtraHalf = UserMapper.findById(userId).hasExtraHalfHour()
                if ((allowForceGroup && services.size == 1) || allowExtraHalf) {
                    runBlocking {
                        sendTextMessage(
                            ctx.chatId!!.toChatId(),
                            "Сохранено",
                            replyMarkup = inlineKeyboard {
                                if (allowForceGroup) row {
                                    dataButton(
                                        "Сделать групповым занятием",
                                        "forceGroupAdd-${ctx.id}"
                                    )
                                }
                                if (allowExtraHalf) row {
                                    dataButton(
                                        "Сделать полтора часа",
                                        "addExtraHalfHour-${ctx.id}"
                                    )
                                }
                            }
                        )
                        val tmp =
                            sendTextMessage(ctx.chatId!!.toChatId(), "temp", replyMarkup = ReplyKeyboardRemove())
                        deleteMessage(tmp.chat, tmp.messageId)
                    }
                } else {
                    runBlocking {
                        sendTextMessage(
                            ctx.chatId!!.toChatId(),
                            "Сохранено",
                            replyMarkup = ReplyKeyboardRemove()
                        )
                    }
                }
            },
            onCancel = { ctx ->
                ctx.photoReport?.let { key ->
                    MinioService.delete(key).onFailure {
                        runBlocking { sendTextMessage(ctx.chatId!!.toChatId(), "Ошибка при удалении фотографии") }
                    }
                }
                sendTextMessage(
                    ctx.chatId!!.toChatId(),
                    "Отменено",
                    replyMarkup = ReplyKeyboardRemove()
                )
            }
        )
    }
}

suspend fun BehaviourContext.sendLog(services: List<Service>, userId: Long) {
    val service = services.first()
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "timeDelete-${service.id}") }
        row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${service.id}") }
        if (services.size == 1) row { dataButton("Сделать групповым занятием", "forceGroupAdd-${service.id}") }
    }

    val log = buildString {
        appendLine("#занятие")
        appendLine("Время: ${service.dateTime.formatDateTime()}")
        appendLine("Предмет: ${ServicesMapper.getNameById(service.serviceId).hashtag()}")
        appendLine(
            "ученик: " + services.toClientIds()
                .joinToString(", ") { "#${ClientMapper.getFioById(it).replace(" ", "_")}" })
        appendLine("Стоимость: ${service.amount}")
        appendLine("Преподаватель: ${UserMapper.findById(userId)?.name.hashtag()}")
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
