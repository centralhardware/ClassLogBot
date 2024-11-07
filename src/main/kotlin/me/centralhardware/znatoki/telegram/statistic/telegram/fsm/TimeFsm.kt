package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import java.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.extensions.process
import me.centralhardware.znatoki.telegram.statistic.extensions.switchToInlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.extensions.yesNoKeyboard
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine

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
        createStdLibStateMachine(
            "time",
            creationArguments = buildCreationArguments { isUndoEnabled = true },
        ) {
            logger = fsmLog
            addInitialState(TimeStates.Initial) {
                transition<UpdateEvent> { targetState = TimeStates.Subject }
            }
            addState(TimeStates.Subject) {
                transition<UpdateEvent> { targetState = TimeStates.Fio }
                onEntry { processState(it, this, ::subject) }
            }
            addState(TimeStates.Fio) {
                transition<UpdateEvent> { targetState = TimeStates.Amount }
                onEntry { processState(it, this, ::fio) }
            }
            addState(TimeStates.Amount) {
                transition<UpdateEvent> { targetState = TimeStates.Properties }
                onEntry { processState(it, this, ::amount) }
            }
            addState(TimeStates.Properties) {
                transition<UpdateEvent> { targetState = TimeStates.Confirm }
                onEntry { processState(it, this, ::property) }
            }
            addFinalState(TimeStates.Confirm) { onEntry { process(it, ::confirm) } }
            onFinished { removeFromStorage(it) }
        }

    private suspend fun subject(
        message: CommonMessage<MessageContent>,
        builder: ServiceBuilder,
    ): Boolean {
        val userId = message.userId()
        val user = UserMapper.findById(userId)!!

        if (user.services.size == 1) {
            builder.serviceId = user.services.first()
            return true
        }

        return validateService(message.text!!)
            .mapLeft(mapError(message))
            .map { service ->
                builder.serviceId = ServicesMapper.getServiceId(service)!!

                bot.sendTextMessage(
                    message.chat,
                    "Введите фио. /complete - для окончания ввода\nнажмите для поиска фио",
                    replyMarkup = switchToInlineKeyboard,
                )
            }
            .isRight()
    }

    private suspend fun fio(
        message: CommonMessage<MessageContent>,
        builder: ServiceBuilder,
    ): Boolean {
        val text = message.text!!
        if (!ServicesMapper.isAllowMultiplyClients(builder.serviceId!!)!!) {
            return validateFio(text)
                .mapLeft(mapError(message))
                .map {
                    val id = text.split(" ").first().toInt()
                    if (builder.clientIds.contains(id)) {
                        bot.sendTextMessage(message.chat, "Данное ФИО уже добавлено")
                        return false
                    }

                    builder.clientIds.add(id)

                    bot.sendTextMessage(message.chat, "Введите стоимость занятия")
                }
                .isRight()
        } else {
            if (text == "/complete") {
                if (builder.clientIds.isEmpty()) {
                    bot.sendTextMessage(message.chat, "Необходимо ввести как минимум одно ФИО")
                    return false
                }
                bot.sendTextMessage(message.chat, "Введите стоимость занятия")
                return true
            }
            validateFio(text).mapLeft(mapError(message)).map {
                val id = text.split(" ").first().toInt()
                if (builder.clientIds.contains(id)) {
                    runBlocking { bot.sendTextMessage(message.chat, "Данное ФИО уже добавлено") }
                    return false
                }

                builder.clientIds.add(id)

                bot.sendTextMessage(message.chat, "ФИО сохранено")
            }
            return false
        }
    }

    private suspend fun amount(
        message: CommonMessage<MessageContent>,
        builder: ServiceBuilder,
    ): Boolean {
        val userId = message.userId()
        val znatokiUser = UserMapper.findById(userId)!!
        return validateAmount(message.text!!)
            .mapLeft(mapError(message))
            .map { amount ->
                builder.amount = amount

                if (ConfigMapper.serviceProperties().isEmpty()) {
                    confirmMessage(message, builder)
                } else {
                    builder.propertiesBuilder =
                        PropertiesBuilder(
                            ConfigMapper.serviceProperties().propertyDefs.toMutableList()
                        )

                    val next = builder.nextProperty()!!

                    if (next.second.isNotEmpty()) {
                        bot.sendTextMessage(
                            message.chat,
                            next.first,
                            replyMarkup =
                                replyKeyboard { next.second.forEach { row { simpleButton(it) } } },
                        )
                    } else {
                        bot.sendTextMessage(message.chat, next.first)
                    }
                }
            }
            .isRight()
    }

    suspend fun property(message: CommonMessage<MessageContent>, builder: ServiceBuilder): Boolean {
        if (ConfigMapper.serviceProperties().isEmpty()) return true

        return builder.propertiesBuilder!!.process(message) { properties ->
            builder.properties = properties
            runBlocking { confirmMessage(message, builder) }
        }
    }

    private suspend fun confirmMessage(
        message: CommonMessage<MessageContent>,
        builder: ServiceBuilder,
    ) {
        bot.sendTextMessage(
            message.chat,
            """
                            услуга: ${ServicesMapper.getNameById(builder.serviceId!!)}
                            ФИО: ${
            builder.clientIds.stream().map { ClientMapper.getFioById(it) }.toList().joinToString(";")
        }
                            стоимость: ${builder.amount}
                            Сохранить?
                            """
                .trimIndent(),
            replyMarkup = yesNoKeyboard,
        )
    }
}

private suspend fun confirm(
    message: CommonMessage<MessageContent>,
    builder: ServiceBuilder,
): Boolean {
    when (message.text) {
        "да" -> {
            builder.id = UUID.randomUUID()

            val services = builder.build()

            services.forEach { ServiceMapper.insert(it) }

            sendLog(services, message.userId())
            if (UserMapper.hasForceGroup(message.from!!.id.chatId.long)) {
                bot.sendTextMessage(
                    message.chat,
                    "Сохранено",
                    replyMarkup =
                        inlineKeyboard {
                            row {
                                dataButton(
                                    "сделать групповым занятием",
                                    "forceGroupAdd-${builder.id}",
                                )
                            }
                        },
                )
                val msg =
                    bot.sendTextMessage(message.chat, "temp", replyMarkup = ReplyKeyboardRemove())
                bot.deleteMessage(msg.chat, msg.messageId)
            } else {
                bot.sendTextMessage(message.chat, "Сохранено", replyMarkup = ReplyKeyboardRemove())
            }
            Trace.save("commitTime", mapOf("id" to builder.id.toString()))
        }
        "нет" -> {
            builder.properties!!
                .filter { it.type is Photo }
                .forEach { photo ->
                    MinioService.delete(photo.value!!).onFailure {
                        coroutineScope {
                            launch {
                                bot.sendTextMessage(message.chat, "Ошибка при удаление фотографии")
                            }
                        }
                    }
                }
            bot.sendTextMessage(message.chat, "Отменено", replyMarkup = ReplyKeyboardRemove())
            Trace.save("rollbackTime", mapOf())
        }
    }
    return true
}

private suspend fun sendLog(services: List<Service>, userId: Long) {
    val service = services.first()
    val keyboard = inlineKeyboard {
        row { dataButton("удалить", "timeDelete-${service.id}") }
        if (services.size == 1) {
            row { dataButton("сделать групповым занятием", "forceGroupAdd-${service.id}") }
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
        bot.sendActionUploadPhoto(ConfigMapper.logChat())
        service.properties
            .filter { it.type is Photo }
            .forEach { photo ->
                bot.sendPhoto(
                    ConfigMapper.logChat(),
                    InputFile.fromInput("Отчет") {
                        MinioService.get(photo.value!!)
                            .onFailure {
                                runBlocking {
                                    bot.sendTextMessage(
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
        bot.sendTextMessage(ConfigMapper.logChat(), log, replyMarkup = keyboard)
    }
}

suspend fun startTimeFsm(message: CommonMessage<MessageContent>): ServiceBuilder {
    val userId = message.userId()
    val user = UserMapper.findById(userId)!!
    val builder = ServiceBuilder()
    builder.chatId = userId
    if (UserMapper.findById(userId)!!.services.size == 1) {
        builder.serviceId = UserMapper.findById(userId)!!.services.first()
    }

    when {
        user.services.size != 1 -> {
            bot.sendTextMessage(
                message.chat,
                "Выберите предмет",
                replyMarkup =
                    replyKeyboard {
                        user.services.forEach {
                            row { ServicesMapper.getNameById(it)?.let { simpleButton(it) } }
                        }
                    },
            )
        }
        else -> {
            bot.sendTextMessage(
                message.chat,
                "Введите фио. /complete - для окончания ввода\nнажмите для поиска фио",
                replyMarkup = switchToInlineKeyboard,
            )
        }
    }
    return builder
}
