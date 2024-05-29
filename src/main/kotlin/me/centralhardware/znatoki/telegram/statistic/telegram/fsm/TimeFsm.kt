package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.switchInlineQueryField
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.FinalState
import ru.nsk.kstatemachine.state.addFinalState
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.onFinished
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import java.util.*

sealed class TimeStates : DefaultState() {
    object Initial : TimeStates()
    object Subject : TimeStates()
    object Fio : TimeStates()
    object Amount : TimeStates()
    object Properties : TimeStates()
    object Confirm : TimeStates(), FinalState
}

class TimeFsm(builder: ServiceBuilder) : Fsm<ServiceBuilder>(builder) {
    override fun createFSM(): StateMachine =
        createStdLibStateMachine("time", creationArguments = StateMachine.CreationArguments(isUndoEnabled = true)) {
            logger = fsmLog
            addInitialState(TimeStates.Initial) {
                transition<UpdateEvent> {
                    targetState = TimeStates.Subject
                }
            }
            addState(TimeStates.Subject) {
                transition<UpdateEvent> {
                    targetState = TimeStates.Fio
                }
                onEntry { processState(it, this, ::subject) }
            }
            addState(TimeStates.Fio) {
                transition<UpdateEvent> {
                    targetState = TimeStates.Amount
                }
                onEntry { processState(it, this, ::fio) }
            }
            addState(TimeStates.Amount) {
                transition<UpdateEvent> {
                    targetState = TimeStates.Properties
                }
                onEntry { processState(it, this, ::amount) }
            }
            addState(TimeStates.Properties) {
                transition<UpdateEvent> {
                    targetState = TimeStates.Confirm
                }
                onEntry { processState(it, this, ::property) }
            }
            addFinalState(TimeStates.Confirm) {
                onEntry { process(it, ::confirm) }
            }
            onFinished { removeFromStorage(it) }
        }


    private suspend fun subject(message: CommonMessage<MessageContent>, builder: ServiceBuilder): Boolean {
        val userId = message.userId()
        val user = UserMapper.getById(userId)!!
        val znatokiUser = UserMapper.getById(userId)!!

        if (user.services.size == 1) {
            builder.serviceId = user.services.first()
            return true
        }

        return validateService(Pair(message.text!!, znatokiUser.organizationId))
            .mapLeft(mapError(message))
            .map { service ->
                builder.serviceId = ServicesMapper.getServiceId(znatokiUser.organizationId, service)!!

                bot.sendTextMessage(message.chat, "Введите фио. /complete - для окончания ввода")

                bot.sendTextMessage(message.chat, "нажмите для поиска фио", replyMarkup = inlineKeyboard {
                    row { switchInlineQueryField }
                })
            }.isRight()
    }

    private suspend fun fio(message: CommonMessage<MessageContent>, builder: ServiceBuilder): Boolean {
        val text = message.text!!
        if (!ServicesMapper.isAllowMultiplyClients(builder.serviceId)!!) {
            return validateFio(text)
                .mapLeft(mapError(message))
                .map {
                    val id = text.split(" ").first().toInt()
                    if (builder.clientIds.contains(id)) {
                        bot.sendTextMessage(message.chat, "Данное ФИО уже добавлено")
                        return false
                    }

                    builder.clientId(id)

                    bot.sendTextMessage(message.chat, "Введите стоимость занятия")
                }.isRight()
        } else {
            if (text == "/complete") {
                if (builder.clientIds.isEmpty()) {
                    bot.sendTextMessage(message.chat, "Необходимо ввести как минимум одно ФИО")
                    return false
                }
                bot.sendTextMessage(message.chat, "Введите стоимость занятия")
                return true
            }
            validateFio(text)
                .mapLeft(mapError(message))
                .map {
                    val id = text.split(" ").first().toInt()
                    if (builder.clientIds.contains(id)) {
                        runBlocking { bot.sendTextMessage(message.chat, "Данное ФИО уже добавлено") }
                        return false
                    }

                    builder.clientId(id)

                    bot.sendTextMessage(message.chat, "ФИО сохранено")
                }
            return false
        }
    }

    private suspend fun amount(message: CommonMessage<MessageContent>, builder: ServiceBuilder): Boolean {
        val userId = message.userId()
        val znatokiUser = UserMapper.getById(userId)!!
        val org = OrganizationMapper.getById(znatokiUser.organizationId)!!
        return validateAmount(message.text!!)
            .mapLeft(mapError(message))
            .map { amount ->
                builder.amount = amount

                if (org.serviceCustomProperties.isEmpty()) {
                    confirmMessage(message, builder)
                } else {
                    builder.propertiesBuilder =
                        PropertiesBuilder(org.serviceCustomProperties.propertyDefs.toMutableList())

                    val next = builder.nextProperty()!!

                    if (next.second.isNotEmpty()) {
                        bot.sendTextMessage(message.chat, next.first, replyMarkup = replyKeyboard {
                            next.second.forEach { row { simpleButton(it) } }
                        })
                    } else {
                        bot.sendTextMessage(message.chat, next.first)
                    }
                }
            }.isRight()
    }

    suspend fun property(message: CommonMessage<MessageContent>, builder: ServiceBuilder): Boolean {
        val userId = message.userId()
        val znatokiUser = UserMapper.getById(userId)!!
        val org = OrganizationMapper.getById(znatokiUser.organizationId)!!
        if (org.serviceCustomProperties.isEmpty()) return true

        return builder.propertiesBuilder.process(
            message
        ) { properties ->
            builder.properties = properties
            runBlocking { confirmMessage(message, builder) }
        }
    }

    private suspend fun confirmMessage(message: CommonMessage<MessageContent>, builder: ServiceBuilder) {
        bot.sendTextMessage(message.chat, """
                            услуга: ${ServicesMapper.getNameById(builder.serviceId)}
                            ФИО: ${
            builder.clientIds.stream().map { ClientMapper.getFioById(it) }.toList().joinToString(";")
        }
                            стоимость: ${builder.amount}
                            Сохранить?
                            """.trimIndent(), replyMarkup = replyKeyboard {
            row { yes() }
            row { no() }
        })
    }
}

private suspend fun confirm(message: CommonMessage<MessageContent>, builder: ServiceBuilder): Boolean {
    val text = message.text
    val userId = message.userId()
    if (text == "да") {
        val serviceId = UUID.randomUUID()

        builder.id = serviceId
        builder.organizationId = UserMapper.getById(userId)!!.organizationId

        val services = builder.build()
        services.forEach {
            ServiceMapper.insertTime(it)

            PaymentMapper.insert(
                Payment(
                    clientId = it.clientId,
                    amount = it.amount * -1,
                    timeId = it.id,
                    organizationId = it.organizationId
                )
            )
        }

        sendLog(services, userId)
        bot.sendTextMessage(message.chat, "Сохранено")
    } else if (text == "нет") {
        builder.properties
            .filter { it.type is Photo }
            .forEach { photo ->
                MinioService.delete(photo.value!!)
                    .onFailure {
                        runBlocking { bot.sendTextMessage(message.chat, "Ошибка при удаление фотографии") }
                    }
            }
        bot.sendTextMessage(message.chat, "Отменено")
    }
    return true
}

private suspend fun sendLog(services: List<Service>, userId: Long) {
    getLogUser(userId)?.let { logId ->
        val service = services.first()
        val keyboard = inlineKeyboard {
            row { dataButton("удалить", "timeDelete-${service.id}") }
        }

        val log = """
                    #занятие
                    Время: ${service.dateTime.formatDateTime()}
                    Предмет: ${ServicesMapper.getNameById(service.serviceId).hashtag()}
                    ${OrganizationMapper.getById(service.organizationId)?.clientName}: ${
            services.toClientIds().joinToString(", ") {
                "#${ClientMapper.getFioById(it).replace(" ", "_")}(${
                    PaymentMapper.getCredit(
                        service.clientId
                    )
                })"
            }
        }
                    Стоимость: ${service.amount}
                    Преподаватель: ${UserMapper.getById(userId)?.name.hashtag()}
                    ${service.properties.print()}
                    """.trimIndent()

        val hasPhoto = service.properties.count { it.type is Photo }

        if (hasPhoto == 1) {
            service.properties.filter { it.type is Photo }
                .forEach { photo ->
                    bot.sendDocument(
                        logId,
                        InputFile.fromInput("Отчет") {
                            MinioService.get(photo.value!!).onFailure {
                                runBlocking {
                                    bot.sendTextMessage(
                                        logId,
                                        "Ошибка во время отправки лога"
                                    )
                                }
                            }.getOrThrow()
                        },
                        replyMarkup = keyboard
                    )
                }
        } else {
            bot.sendTextMessage(logId, log, replyMarkup = keyboard)
        }
    }
}


suspend fun startTimeFsm(message: CommonMessage<MessageContent>): ServiceBuilder {
    val userId = message.userId()
    val user = UserMapper.getById(userId)!!
    val builder = ServiceBuilder()
    builder.chatId = userId
    if (UserMapper.getById(userId)!!.services.size == 1) {
        builder.serviceId = UserMapper.getById(userId)!!.services.first()
    }

    when {
        user.services.size != 1 -> {
            bot.sendTextMessage(message.chat, "Выберите предмет", replyMarkup = replyKeyboard {
                user.services.forEach { row { ServicesMapper.getNameById(it)?.let { simpleButton(it) } } }
            })
        }

        else -> {
            bot.sendTextMessage(message.chat, "Введите фио. /complete - для окончания ввода")
            bot.sendTextMessage(message.chat, "нажмите для поиска фио", replyMarkup = switchToInlineKeyboard)
        }
    }
    return builder
}