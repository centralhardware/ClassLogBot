package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.inlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.*
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
    override fun createFSM(): StateMachine = createStdLibStateMachine("time", creationArguments = StateMachine.CreationArguments(isUndoEnabled = true)) {
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


    fun subject(update: Update, builder: ServiceBuilder): Boolean {
        val userId = update.userId()
        val user = userMapper().getById(userId)!!
        val znatokiUser = userMapper().getById(userId)!!

        if (user.services.size == 1) {
            builder.serviceId = user.services.first()
            return true
        }

        return serviceValidator().validate(Pair(update.message.text, znatokiUser.organizationId))
            .mapLeft(mapError(userId))
            .map { service ->
                builder.serviceId = servicesMapper().getServiceId(znatokiUser.organizationId, service)!!

                sender().sendText("Введите фио. /complete - для окончания ввода", userId)

                sender().send {
                    execute(inlineKeyboard {
                        chatId(userId)
                        text("нажмите для поиска фио")
                        row { switchToInline() }
                    }.build())
                }
            }.isRight()
    }

    fun fio(update: Update, builder: ServiceBuilder): Boolean {
        val text = update.message.text
        val userId = update.userId()
        if (!servicesMapper().isAllowMultiplyClients(builder.serviceId)!!) {
            return fioValidator().validate(text)
                .mapLeft(mapError(userId))
                .map {
                    val id = text.split(" ").first().toInt()
                    if (builder.clientIds.contains(id)) {
                        sender().sendText("Данное ФИО уже добавлено", userId)
                        return false
                    }

                    builder.clientId(id)

                    sender().sendText("Введите стоимость занятия", userId)
                }.isRight()
        } else {
            if (Objects.equals(text, "/complete")) {
                if (builder.clientIds.isEmpty()) {
                    sender().sendText("Необходимо ввести как минимум одно ФИО", userId)
                    return false
                }
                sender().sendText("Введите стоимость занятия", userId)
                return true
            }
            fioValidator().validate(text)
                .mapLeft { error -> sender().sendText(error, userId) }
                .map {
                    val id = text.split(" ").first().toInt()
                    if (builder.clientIds.contains(id)) {
                        sender().sendText("Данное ФИО уже добавлено", userId)
                        return false
                    }

                    builder.clientId(id)

                    sender().sendText("ФИО сохранено", userId)
                }
            return false
        }
    }

    fun amount(update: Update, builder: ServiceBuilder): Boolean {
        val text = update.message.text
        val userId = update.userId()
        val znatokiUser = userMapper().getById(userId)!!
        val org = organizationMapper().getById(znatokiUser.organizationId)!!
        return amountValidator().validate(text)
            .mapLeft(mapError(userId))
            .map { amount ->
                builder.amount = amount

                if (org.serviceCustomProperties.isEmpty()) {
                    confirmMessage(userId, builder)
                } else {
                    builder.propertiesBuilder =
                        PropertiesBuilder(org.serviceCustomProperties.propertyDefs.toMutableList())

                    val next = builder.nextProperty()!!

                    if (next.second.isNotEmpty()) {
                        sender().send {
                            execute(replyKeyboard {
                                chatId(userId)
                                text(next.first)
                                next.second.forEach { row { btn(it) } }
                            }.build())
                        }
                    } else {
                        sender().sendText(next.first, userId)
                    }
                }
            }.isRight()
    }

    fun property(update: Update, builder: ServiceBuilder): Boolean {
        val userId = update.userId()
        val znatokiUser = userMapper().getById(userId)!!
        val org = organizationMapper().getById(znatokiUser.organizationId)!!
        if (org.serviceCustomProperties.isEmpty()) return true

        return builder.propertiesBuilder.process(
            update
        ) { properties ->
            builder.properties = properties
            confirmMessage(userId, builder)
        }
    }

    fun confirmMessage(userId: Long, builder: ServiceBuilder) {
        sender().send {
            execute(replyKeyboard {
                chatId(userId)
                text(
                    """
                            услуга: ${servicesMapper().getNameById(builder.serviceId)}
                            ФИО: ${
                        builder.clientIds.stream().map { clientService().getFioById(it) }.toList().joinToString(";")
                    }
                            стоимость: ${builder.amount}
                            Сохранить?
                            """.trimIndent()
                )
                row { yes() }
                row { no() }
            }.build())
        }
    }

    fun confirm(update: Update, builder: ServiceBuilder): Boolean {
        val text = update.message.text
        val userId = update.userId()
        if (Objects.equals(text, "да")) {
            val serviceId = UUID.randomUUID()

            builder.id = serviceId
            builder.organizationId = userMapper().getById(userId)!!.organizationId

            val services = builder.build()
            services.forEach {
                serviceMapper().insertTime(it)

                paymentMapper().insert(
                    Payment(
                        clientId = it.clientId,
                        amount = it.amount * -1,
                        timeId = it.id,
                        organizationId = it.organizationId
                    )
                )
            }

            sendLog(services, userId)
            sender().sendText("Сохранено", userId)
        } else if (Objects.equals(text, "нет")) {
            builder.properties
                .filter { it.type is Photo }
                .forEach { photo ->
                    minioService().delete(photo.value!!)
                        .onFailure {
                            sender().sendText("Ошибка при удаление фотографии", update.userId())
                        }
                }
            sender().sendText("Отменено", userId)
        }
        return true
    }

    fun sendLog(services: List<Service>, userId: Long) {
        getLogUser(userId)?.let { logId ->
            val service = services.first()
            val keyboard = inlineKeyboard {
                text("?")
                row { btn("удалить", "timeDelete-${service.id}") }
            }.buildReplyMarkup()

            val log = """
                    #занятие
                    Время: ${service.dateTime.formatDateTime()}
                    Предмет: ${servicesMapper().getNameById(service.serviceId).hashtag()}
                    ${organizationMapper().getById(service.organizationId)?.clientName}: ${
                services.toClientIds().joinToString(", ") {
                    "#${clientService().getFioById(it).replace(" ", "_")}(${
                        paymentMapper().getCredit(
                            service.clientId
                        )
                    })"
                }
            }
                    Стоимость: ${service.amount}
                    Преподаватель: ${userMapper().getById(userId)?.name.hashtag()}
                    ${service.properties.print()}
                    """.trimIndent()

            val hasPhoto = service.properties.count { it.type is Photo }

            if (hasPhoto == 1) {
                service.properties.filter { it.type is Photo }
                    .forEach { photo ->
                        val sendPhoto = SendPhoto.builder()
                            .photo(InputFile(minioService().get(photo.value!!).onFailure {
                                sender().sendText("Ошибка во время отправки лога", userId)
                            }.getOrThrow(), "отчет"))
                            .chatId(logId)
                            .caption(log)
                            .replyMarkup(keyboard)
                            .build()
                        sender().send { execute(sendPhoto) }
                    }
            } else {
                val message = SendMessage
                    .builder()
                    .chatId(logId)
                    .text(log)
                    .replyMarkup(keyboard)
                sender().send { execute(message.build()) }
            }
        }
    }


}


fun startTimeFsm(update: Update): ServiceBuilder {
    val userId = update.userId()
    val user = userMapper().getById(userId)!!
    val builder = ServiceBuilder()
    builder.chatId = userId
    if (userMapper().getById(userId)!!.services.size == 1) {
        builder.serviceId = userMapper().getById(userId)!!.services.first()
    }

    when {
        user.services.size != 1 -> {
            sender().send {
                execute(replyKeyboard {
                    text("Выберите предмет")
                    chatId(user.id)
                    user.services.forEach { row { servicesMapper().getNameById(it)?.let { btn(it) } } }
                }.build())
            }
        }

        else -> {
            sender().sendText("Введите фио. /complete - для окончания ввода", userId)
            sender().send {
                execute(inlineKeyboard {
                    text("нажмите для поиска фио")
                    chatId(userId)
                    row { switchToInline() }
                }.build())
            }
        }
    }
    return builder
}