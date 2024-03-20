package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.inlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.*
import kotlin.math.abs

sealed class PaymentStates: DefaultState(){
    object Initial: PaymentStates()
    object Pupil: PaymentStates()
    object Service: PaymentStates()
    object Amount: PaymentStates()
    object Property: PaymentStates()
    object Confirm: PaymentStates(), FinalState
}

fun createPaymentFsm() = createStdLibStateMachine("payment", enableUndo = true){
    logger = fsmLog
    addInitialState(PaymentStates.Initial){
        transition<UpdateEvent.UpdateEvent> {
            targetState = PaymentStates.Pupil
        }
    }
    addState(PaymentStates.Pupil){
        transition<UpdateEvent.UpdateEvent> {
            targetState = PaymentStates.Service
        }
        onEntry { processState(it, this, ::pupil) }
    }
    addState(PaymentStates.Service){
        transition<UpdateEvent.UpdateEvent> {
            targetState = PaymentStates.Amount
        }
        onEntry { processState(it, this, ::service) }
    }
    addState(PaymentStates.Amount){
        transition<UpdateEvent.UpdateEvent> {
            targetState = PaymentStates.Property
        }
        onEntry { processState<PaymentBuilder>(it, this, ::amount) }
    }
    addState(PaymentStates.Property){
        transition<UpdateEvent.UpdateEvent> {
            targetState = PaymentStates.Confirm
        }
        onEntry { processState<PaymentBuilder>(it, this, ::property) }
    }
    addState(PaymentStates.Confirm){
        transition<UpdateEvent.UpdateEvent> {
            targetState = PaymentStates.Property
        }
        onEntry { process<PaymentBuilder>(it, ::confirm) }
    }
    onFinished { removeFromStorage<PaymentBuilder>(it) }
}

fun pupil(update: Update, builder: PaymentBuilder): Boolean{
    val userId = update.userId()
    return fioValidator().validate(update.message.text)
        .mapLeft(mapError(userId))
        .map { fio ->
            val id = fio.split(" ")[0].toInt()

            builder.clientId = id
            sender().send{
                execute(replyKeyboard {
                    chatId(userId)
                    text("Выберите предмет")
                    userMapper().getById(userId)?.services?.forEach {
                        row { servicesMapper().getNameById(it)?.let { name -> btn(name) } }
                    }
                }.build())
            }
        }.isRight()
}

fun startPaymentFsm(update: Update): PaymentBuilder{
    sender().sendText("Введите фио", update.userId())
    val builder = PaymentBuilder()
    sender().send{
        execute(inlineKeyboard {
            chatId(update.userId())
            text("нажмите для поиска фио")
            row { switchToInline() }
        }.build())
    }
    return builder
}

fun service(update: Update, builder: PaymentBuilder): Boolean{
    val userId = update.userId()
    val znatokiUser = userMapper().getById(userId)!!
    return serviceValidator().validate(Pair(update.message.text, znatokiUser.organizationId))
        .mapLeft(mapError(userId))
        .map { service ->
            builder.serviceId = servicesMapper().getServiceId(znatokiUser.organizationId, service)!!

            sender().sendText("Введите сумму оплаты", userId)
        }.isRight()
}

fun amount(update: Update, builder: PaymentBuilder): Boolean{
    val userId = update.userId()
    val znatokiUser = userMapper().getById(userId)!!
    return amountValidator().validate(update.message.text)
        .mapLeft(mapError(userId))
        .map { amount ->
            builder.amount = amount
            val org = organizationMapper().getById(znatokiUser.organizationId)!!
            if (org.paymentCustomProperties.isEmpty()
            ) {
                sender().send{
                    execute(replyKeyboard {
                        chatId(userId)
                        text(
                            builder.let {
                                """
                                        ФИО: ${
                                    clientService().findById(it.clientId)?.fio()
                                }
                                        Оплата: ${it.amount}
                                        """
                            }
                        )
                        row { yes() }
                        row { no() }
                    }.build())
                }
            } else {
                builder.propertiesBuilder = PropertiesBuilder(org.paymentCustomProperties.propertyDefs.toMutableList())
                val next = builder.nextProperty()!!
                if (next.second.isNotEmpty()) {
                    sender().send{
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

fun property(update: Update, builder: PaymentBuilder): Boolean{
    val userId = update.userId()
    val znatokiUser = userMapper().getById(userId)!!
    val org = organizationMapper().getById(znatokiUser.organizationId)!!

    if (org.paymentCustomProperties.isEmpty()) return true

    return processCustomProperties(
        update,
        builder.propertiesBuilder
    ) { properties ->
        builder.properties = properties
        sender().send{
            execute(replyKeyboard {
                chatId(userId)
                text(
                    """
                                        ФИО: ${
                        clientService().findById(builder.clientId)?.fio()
                    }
                                        Оплата: ${builder.amount}
                                        """
                )

                row { yes() }
                row { no() }
            }.build())
        }
    }
}

fun confirm(update: Update, builder: PaymentBuilder): Boolean{
    val userId = update.userId()
    if (update.message.text == "да") {
        builder.organizationId = userMapper().getById(userId)!!.organizationId
        builder.chatId = userId
        val payment = builder.build()
        val paymentId = paymentMapper().insert(payment)
        sendLog(payment, paymentId, userId)
        if (paymentMapper().paymentExists(payment.clientId)) {
            paymentMapper().insert(
                Payment(
                    chatId = userId,
                    clientId = payment.clientId,
                    amount = (abs(paymentMapper().getCredit(payment.clientId)) + (payment.amount * 2)).toInt(),
                    organizationId = payment.organizationId
                )
            )
        }
        sender().sendText("Сохранено", userId)
    } else if (update.message.text == "нет") {
        builder.properties.stream()
            .filter { it.type is Photo }
            .forEach { photo ->
                minioService().delete(photo.value!!)
                    .onFailure { sender().sendText("Ошибка при удаление фотографии", update.userId()) }
            }
        sender().sendText("Отменено", userId)
    }
    return true
}

fun sendLog(payment: Payment, paymentId: Int, userId: Long) {
    getLogUser(userId)?.let { logId ->
        val keyboard = inlineKeyboard {
            text("?")
            row { btn("удалить", "paymentDelete-${paymentId}") }
        }.buildReplyMarkup()
        val text = """
                #оплата
                Время: ${payment.dateTime.formatDateTime()}
                Организация: ${organizationMapper().getById(payment.organizationId)!!.name.hashtag()}
                Клиент: ${clientService().findById(payment.clientId)?.fio().hashtag()}
                Предмет: ${servicesMapper().getNameById(payment.serviceId!!)}
                Оплата: ${payment.amount}
                Оплатил: ${userMapper().getById(userId)!!.name.hashtag()}
                ${payment.properties.print()}
            """.trimIndent()
        val hasPhoto = payment.properties.stream().filter { it.type is Photo }.count()
        if (hasPhoto == 1L) {
            payment.properties
                .filter { it.type is Photo }
                .forEach { photo ->
                    val sendPhoto = SendPhoto.builder()
                        .photo(
                            InputFile(
                                minioService().get(photo.value!!).onFailure {
                                    sender().sendText(
                                        "Ошибка во время отправки лога",
                                        userId
                                    )
                                }.getOrThrow(),
                                "отчет"
                            )
                        )
                        .chatId(logId)
                        .caption(text)
                        .replyMarkup(keyboard)
                        .build()
                    sender().send{execute(sendPhoto)}
                }
        } else {
            val message = SendMessage.builder()
                .text(text)
                .chatId(logId)
                .replyMarkup(keyboard)
                .build()
            sender().send{execute(message)}
        }
    }
}

