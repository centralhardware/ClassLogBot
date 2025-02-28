package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.user
import me.centralhardware.znatoki.telegram.statistic.validateAmount
import me.centralhardware.znatoki.telegram.statistic.validateFio
import me.centralhardware.znatoki.telegram.statistic.validateService
import java.util.*

sealed interface TimeState : State
data class TimeInitial(override val context: Long) : TimeState
data class TimeSubject(override val context: ServiceBuilder, val userId: Long) : TimeState
data class TimeFio(override val context: ServiceBuilder, val userId: Long) : TimeState
data class TimeAmount(override val context: ServiceBuilder, val userId: Long) : TimeState
data class TimeProperties(override val context: ServiceBuilder, val userId: Long) : TimeState
data class TimeConfirm(override val context: ServiceBuilder, val userId: Long) : TimeState

suspend fun BehaviourContext.buildTimeFsm(flow: Flow<Update>, chatId: Long): DefaultBehaviourContextWithFSM<TimeState> {
    val hx = buildBehaviourWithFSM(
        flow,
        onStateHandlingErrorHandler = { state, e ->
            e.printStackTrace()
            state
        }) {
        timeInitial()
        subject()
        timeFio()
        amount()
        property()
        confirm()
    }
    hx.startChain(TimeInitial(chatId))
    hx.start()
    return hx
}

private fun DefaultBehaviourContextWithFSM<TimeState>.timeInitial() {
    strictlyOn<TimeInitial> {
        val builder = ServiceBuilder()
        builder.chatId = it.context
        if (data.user.services.size == 1) {
            builder.serviceId = data.user.services.first()
        }

        when {
            data.user.services.size != 1 -> {
                TimeSubject(builder, it.context)
            }

            else -> {
                TimeFio(builder, it.context)
                sendTextMessage(
                    it.context.toChatId(),
                    "Введите фио. /complete - для окончания ввода\nнажмите для поиска фио",
                    replyMarkup = switchToInlineKeyboard,
                )
            }
        }
        it
    }
}

private fun DefaultBehaviourContextWithFSM<TimeState>.subject() {
    strictlyOn<TimeSubject> {
        sendTextMessage(
            it.userId.toChatId(),
            "Выберите предмет",
            replyMarkup =
                replyKeyboard {
                    data.user.services.forEach {
                        row { ServicesMapper.getNameById(it)?.let { simpleButton(it) } }
                    }
                },
        )

        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()


        val user = UserMapper.findById(it.context.chatId!!)!!

        if (user.services.size == 1) {
            it.context.serviceId = user.services.first()
            it
        }

        validateService(contentMessage.text!!)
            .mapLeft(mapError(contentMessage))
            .map { service ->
                it.context.serviceId = ServicesMapper.getServiceId(service)!!
                TimeFio(it.context, it.userId)
            }
        it
    }
}

private fun DefaultBehaviourContextWithFSM<TimeState>.timeFio() {

    strictlyOn<TimeFio> {
        sendTextMessage(
            it.context.chatId!!.toChatId(),
            "Введите фио. /complete - для окончания ввода\nнажмите для поиска фио",
            replyMarkup = switchToInlineKeyboard,
        )
        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        val text = contentMessage.text!!
        if (!ServicesMapper.isAllowMultiplyClients(it.context.serviceId!!)!!) {
            validateFio(text)
                .mapLeft(mapError(contentMessage))
                .map { fio ->
                    val id = text.split(" ").first().toInt()
                    if (it.context.clientIds.contains(id)) {
                        sendTextMessage(it.context.chatId!!.toChatId(), "Данное ФИО уже добавлено")
                        it
                    }

                    it.context.clientIds.add(id)

                    TimeAmount(it.context, it.userId)
                }
            it
        } else {
            if (text == "/complete") {
                if (it.context.clientIds.isEmpty()) {
                    bot.sendTextMessage(it.context.chatId!!.toChatId(), "Необходимо ввести как минимум одно ФИО")
                    it
                }
                TimeAmount(it.context, it.userId)
            }
            validateFio(text)
                .mapLeft(mapError(contentMessage))
                .map { fio ->
                val id = text.split(" ").first().toInt()
                    if (it.context.clientIds.contains(id)) {
                        sendTextMessage(it.context.chatId!!.toChatId(), "Данное ФИО уже добавлено")
                        it
                }

                    it.context.clientIds.add(id)

                    bot.sendTextMessage(it.context.chatId!!.toChatId(), "ФИО сохранено")
                    it
            }
            it
        }
    }
}

private fun DefaultBehaviourContextWithFSM<TimeState>.amount() {
    strictlyOn<TimeAmount> {
        sendTextMessage(it.context.chatId!!.toChatId(), "Введите стоимость занятия")

        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        validateAmount(contentMessage.text!!)
            .mapLeft(mapError(contentMessage))
            .map { amount ->
                it.context.amount = amount

                TimeProperties(it.context, it.userId)
            }
        it
    }
}

private fun DefaultBehaviourContextWithFSM<TimeState>.property() {
    strictlyOn<TimeProperties> {
        if (ConfigMapper.serviceProperties().isEmpty()) TimeConfirm(it.context, it.userId)

        it.context.propertiesBuilder =
            PropertiesBuilder(
                ConfigMapper.serviceProperties().propertyDefs.toMutableList()
            )

        val next = it.context.nextProperty()!!

        if (next.second.isNotEmpty()) {
            bot.sendTextMessage(
                it.context.chatId!!.toChatId(),
                next.first,
                replyMarkup =
                    replyKeyboard { next.second.forEach { row { simpleButton(it) } } },
            )
        } else {
            bot.sendTextMessage(it.context.chatId!!.toChatId(), next.first)
        }

        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        if (it.context.propertiesBuilder!!.process(contentMessage, bot) { properties ->
                it.context.properties = properties
            }) {
            TimeConfirm(it.context, it.userId)
        } else {
            it
        }
    }
}

private fun DefaultBehaviourContextWithFSM<TimeState>.confirm() {
    strictlyOn<TimeConfirm> {
        sendTextMessage(
            it.context.chatId!!.toChatId(),
            """
                            услуга: ${ServicesMapper.getNameById(it.context.serviceId!!)}
                            ФИО: ${
                it.context.clientIds.stream().map { ClientMapper.getFioById(it) }.toList().joinToString(";")
            }
                            стоимость: ${it.context.amount}
                            Сохранить?
                            """
                .trimIndent(),
            replyMarkup = yesNoKeyboard,
        )

        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        when (contentMessage.text) {
            "да" -> {
                it.context.id = UUID.randomUUID()

                val services = it.context.build()

                services.forEach { ServiceMapper.insert(it) }

                sendLog(services, it.context.chatId!!)
                if ((UserMapper.findById(it.context.chatId!!)!!.hasForceGroup() && services.size == 1) ||
                    UserMapper.findById(it.context.chatId!!).hasExtraHalfHour()
                ) {
                    sendTextMessage(
                        it.context.chatId!!.toChatId(),
                        "Сохранено",
                        replyMarkup =
                            inlineKeyboard {
                                if (UserMapper.findById(it.context.chatId!!).hasForceGroup()) {
                                    row {
                                        dataButton(
                                            "Сделать групповым занятием",
                                            "forceGroupAdd-${it.context.id}",
                                        )
                                    }
                                }
                                if (UserMapper.findById(it.context.chatId!!).hasExtraHalfHour()) {
                                    row {
                                        dataButton(
                                            "Сделать полтора часа",
                                            "addExtraHalfHour-${it.context.id}",
                                        )
                                    }
                                }
                            },
                    )
                    val msg =
                        sendTextMessage(it.context.chatId!!.toChatId(), "temp", replyMarkup = ReplyKeyboardRemove())
                    deleteMessage(msg.chat, msg.messageId)
                } else {
                    sendTextMessage(it.context.chatId!!.toChatId(), "Сохранено", replyMarkup = ReplyKeyboardRemove())
                }
                Trace.save("commitTime", mapOf("id" to it.context.id.toString()))
            }

            "нет" -> {
                it.context.properties!!
                    .filter { it.type is Photo }
                    .forEach { photo ->
                        MinioService.delete(photo.value!!).onFailure { error ->
                            coroutineScope {
                                launch {
                                    sendTextMessage(it.context.chatId!!.toChatId(), "Ошибка при удаление фотографии")
                                }
                            }
                        }
                    }
                sendTextMessage(it.context.chatId!!.toChatId(), "Отменено", replyMarkup = ReplyKeyboardRemove())
                Trace.save("rollbackTime", mapOf())
            }
        }
        null
    }
}

private suspend fun BehaviourContextWithFSM<in TimeState>.sendLog(services: List<Service>, userId: Long) {
    val service = services.first()
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "timeDelete-${service.id}") }
        row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${service.id}") }
        if (services.size == 1) {
            row { dataButton("Сделать групповым занятием", "forceGroupAdd-${service.id}") }
        }
    }

    val log =
        """
                    #занятие
                    Время: ${service.dateTime.formatDateTime()}
                    Предмет: ${ServicesMapper.getNameById(service.serviceId).hashtag()}
                    ${ConfigMapper.clientName()}: ${
            services.toClientIds().joinToString(", ") {
                "#${ClientMapper.getFioById(it).replace(" ", "_")}"
            }
        }
                    Стоимость: ${service.amount}
                    Преподаватель: ${UserMapper.findById(userId)?.name.hashtag()}
                    ${service.properties.print()}
                    """
            .trimIndent()

    val hasPhoto = service.properties.count { it.type is Photo }

    if (hasPhoto == 1) {
        sendActionUploadPhoto(ConfigMapper.logChat())
        service.properties
            .filter { it.type is Photo }
            .forEach { photo ->
                sendPhoto(
                    ConfigMapper.logChat(),
                    InputFile.fromInput("Отчет") {
                        MinioService.get(photo.value!!)
                            .onFailure {
                                runBlocking {
                                    sendTextMessage(
                                        ConfigMapper.logChat(),
                                        "Ошибка во время отправки лога",
                                    )
                                }
                            }
                            .getOrThrow()
                    },
                    replyMarkup = keyboard,
                    text = log,
                )
            }
    } else {
        sendTextMessage(ConfigMapper.logChat(), log, replyMarkup = keyboard)
    }
}