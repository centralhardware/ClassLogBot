package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.escapeHashtag
import me.centralhardware.znatoki.telegram.statistic.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.print
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.inlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import me.centralhardware.znatoki.telegram.statistic.userId
import me.centralhardware.znatoki.telegram.statistic.utils.*
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.*
import java.util.*

sealed class TimeStates : DefaultState() {
    object Initial : TimeStates()
    object Subject : TimeStates()
    object Fio : TimeStates()
    object Amount : TimeStates()
    object Properties : TimeStates()
    object Confirm : TimeStates(), FinalState
}

fun createTimeFsm() = createStdLibStateMachine("time", enableUndo = true) {
    logger = StateMachine.Logger { lazyMessage ->
        LoggerFactory.getLogger("fsm").info(lazyMessage())
    }
    addInitialState(TimeStates.Initial) {
        transition<UpdateEvent.UpdateEvent> {
            targetState = TimeStates.Subject
        }
    }
    addState(TimeStates.Subject) {
        transition<UpdateEvent.UpdateEvent> {
            targetState = TimeStates.Fio
        }
        onEntry {
            val res = subject(it.argTime().first, it.argTime().second)
            if (!res) machine.undo()
        }
        onExit {  }
    }
    addState(TimeStates.Fio) {
        transition<UpdateEvent.UpdateEvent> {
            targetState = TimeStates.Amount
        }
        onEntry {
            val res = fio(it.argTime().first, it.argTime().second)
            if (!res) machine.undo()
        }
    }
    addState(TimeStates.Amount) {
        transition<UpdateEvent.UpdateEvent> {
            targetState = TimeStates.Properties
        }
        onEntry {
            val res = amount(it.argTime().first, it.argTime().second)
            if (!res) machine.undo()
        }
    }
    addState(TimeStates.Properties) {
        transition<UpdateEvent.UpdateEvent> {
            targetState = TimeStates.Confirm
        }
        onEntry {
            val res = confirm(it.argTime().first, it.argTime().second)
            if (!res) machine.undo()
        }
    }
    addFinalState(TimeStates.Confirm)
}


fun startTimeFsm(update: Update): ServiceBuilder{
    val userId = update.userId()
    val user = userMapper().getById(userId)!!
    val builder = ServiceBuilder()
    builder.chatId =  userId
    if (userMapper().getById(userId)!!.services.size == 1) {
        builder.serviceId = userMapper().getById(userId)!!.services.first()
    }

    when {
        user.services.size != 1 -> {
            sender().send(replyKeyboard {
                text("Выберите предмет")
                chatId(user.id)
                user.services.forEach { row { servicesMapper().getNameById(it)?.let { btn(it) } } }
            }.build())
        }
        else -> {
            sender().sendText("Введите фио. /complete - для окончания ввода", userId)
            sender().send(inlineKeyboard {
                text("нажмите для поиска фио")
                chatId(userId)
                row { switchToInline() }
            })
        }
    }
    return builder
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
        .mapLeft { error -> sender().sendText(error, userId) }
        .map { service ->
            builder.serviceId = servicesMapper().getServiceId(znatokiUser.organizationId, service)!!

            sender().sendText("Введите фио. /complete - для окончания ввода", userId)

            sender().send(inlineKeyboard {
                chatId(userId)
                text("нажмите для поиска фио")
                row { switchToInline() }
            }.build())
        }.isRight()
}

fun fio(update: Update, builder: ServiceBuilder): Boolean {
    val text = update.message.text
    val userId = update.userId()
    if (!servicesMapper().isAllowMultiplyClients(builder.serviceId)!!) {
        return fioValidator().validate(text)
            .mapLeft { error -> sender().sendText(error, userId) }
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
        return fioValidator().validate(text)
            .mapLeft { error -> sender().sendText(error, userId) }
            .map {
                val id = text.split(" ").first().toInt()
                if (builder.clientIds.contains(id)) {
                    sender().sendText("Данное ФИО уже добавлено", userId)
                    return false
                }

                builder.clientId(id)

                sender().sendText("ФИО сохранено", userId)
            }.isRight()
    }
}

fun amount(update: Update, builder: ServiceBuilder): Boolean {
    val text = update.message.text
    val userId = update.userId()
    val znatokiUser = userMapper().getById(userId)!!
    val org = organizationMapper().getById(znatokiUser.organizationId)!!
    return amountValidator().validate(text)
        .mapLeft { error -> sender().sendText(error, userId) }
        .map { amount ->
            builder.amount = amount

            if (org.serviceCustomProperties.isEmpty()) {
                confirmMessage(userId, builder)
            } else {
                builder.propertiesBuilder =
                    PropertiesBuilder(org.serviceCustomProperties.propertyDefs.toMutableList())

                val next = builder.nextProperty()!!

                if (next.second.isNotEmpty()) {
                    sender().send(replyKeyboard {
                        chatId(userId)
                        text(next.first)
                        next.second.forEach { row { btn(it) } }
                    }.build())
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

    var isFinished = false
    processCustomProperties(
        update,
        builder.propertiesBuilder
    ) { properties ->
        builder.properties = properties
        confirmMessage(userId, builder)
        isFinished = true
    }
    return isFinished
}

fun confirmMessage(userId: Long, builder: ServiceBuilder) {
    sender().send(replyKeyboard {
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
        row { btn("да") }
        row { btn("нет") }
    })
}

fun confirm(update: Update, builder: ServiceBuilder): Boolean{
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
                    chatId = userId,
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
                        sender().send("Ошибка при удаление фотографии")
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
                    Предмет: #${servicesMapper().getNameById(service.serviceId).escapeHashtag()}
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
                    Преподаватель: #${userMapper().getById(userId)?.name.escapeHashtag()}
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
                    sender().send(sendPhoto)
                }
        } else {
            val message = SendMessage
                .builder()
                .chatId(logId)
                .text(log)
                .replyMarkup(keyboard)
            sender().send(message.build())
        }
    }
}