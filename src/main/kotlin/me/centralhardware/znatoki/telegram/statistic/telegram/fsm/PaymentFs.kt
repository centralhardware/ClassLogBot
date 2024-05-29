package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.asTextedInput
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import kotlin.math.abs

sealed class PaymentStates : DefaultState() {
    object Initial : PaymentStates()
    object Pupil : PaymentStates()
    object Service : PaymentStates()
    object Amount : PaymentStates()
    object Property : PaymentStates()
    object Confirm : PaymentStates(), FinalState
}

class PaymentFsm(builder: PaymentBuilder) : Fsm<PaymentBuilder>(builder) {
    override fun createFSM(): StateMachine =
        createStdLibStateMachine("payment", creationArguments = StateMachine.CreationArguments(isUndoEnabled = true)) {
            logger = fsmLog
            addInitialState(PaymentStates.Initial) {
                transition<UpdateEvent> {
                    targetState = PaymentStates.Pupil
                }
            }
            addState(PaymentStates.Pupil) {
                transition<UpdateEvent> {
                    targetState = PaymentStates.Service
                }
                onEntry { processState(it, this, ::pupil) }
            }
            addState(PaymentStates.Service) {
                transition<UpdateEvent> {
                    targetState = PaymentStates.Amount
                }
                onEntry { processState(it, this, ::service) }
            }
            addState(PaymentStates.Amount) {
                transition<UpdateEvent> {
                    targetState = PaymentStates.Property
                }
                onEntry { processState(it, this, ::amount) }
            }
            addState(PaymentStates.Property) {
                transition<UpdateEvent> {
                    targetState = PaymentStates.Confirm
                }
                onEntry { processState(it, this, ::property) }
            }
            addState(PaymentStates.Confirm) {
                transition<UpdateEvent> {
                    targetState = PaymentStates.Property
                }
                onEntry { process(it, ::confirm) }
            }
            onFinished { removeFromStorage(it) }
        }

    private suspend fun pupil(message: CommonMessage<MessageContent>, builder: PaymentBuilder): Boolean {
        val userId = message.userId()
        return validateFio(message.content.asTextedInput()!!.text!!)
            .mapLeft(mapError(message))
            .map { fio ->
                val id = fio.split(" ")[0].toInt()

                builder.clientId = id
                bot.sendTextMessage(message.chat, "Выберите предмет", replyMarkup = replyKeyboard {
                    UserMapper.getById(userId)?.services?.forEach {
                        row { ServicesMapper.getNameById(it)?.let { name -> simpleButton(name) } }
                    }
                })
            }.isRight()
    }

    suspend fun service(message: CommonMessage<MessageContent>, builder: PaymentBuilder): Boolean {
        val userId = message.userId()
        val znatokiUser = UserMapper.getById(userId)!!
        return validateService(Pair(message.content.asTextedInput()!!.text!!, znatokiUser.organizationId))
            .mapLeft(mapError(message))
            .map { service ->
                builder.serviceId = ServicesMapper.getServiceId(znatokiUser.organizationId, service)!!

                bot.sendTextMessage(message.chat, "Введите сумму оплаты")
            }.isRight()
    }

    private suspend fun amount(message: CommonMessage<MessageContent>, builder: PaymentBuilder): Boolean {
        val userId = message.userId()
        val znatokiUser = UserMapper.getById(userId)!!
        return validateAmount(message.content.asTextedInput()!!.text!!)
            .mapLeft(mapError(message))
            .map { amount ->
                builder.amount = amount
                val org = OrganizationMapper.getById(znatokiUser.organizationId)!!
                if (org.paymentCustomProperties.isEmpty()
                ) {
                    bot.sendTextMessage(message.chat, builder.let {
                        """
                                        ФИО: ${
                            ClientMapper.findById(it.clientId)?.fio()
                        }
                                        Оплата: ${it.amount}
                                        """
                    }, replyMarkup = replyKeyboard {
                        row { yes() }
                        row { no() }
                    })
                } else {
                    builder.propertiesBuilder =
                        PropertiesBuilder(org.paymentCustomProperties.propertyDefs.toMutableList())
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

    suspend fun property(message: CommonMessage<MessageContent>, builder: PaymentBuilder): Boolean {
        val userId = message.userId()
        val znatokiUser = UserMapper.getById(userId)!!
        val org = OrganizationMapper.getById(znatokiUser.organizationId)!!

        if (org.paymentCustomProperties.isEmpty()) return true

        return builder.propertiesBuilder.process(
            message,
        ) { properties ->
            builder.properties = properties
            runBlocking {
                bot.sendTextMessage(message.chat, """
                                        ФИО: ${
                    ClientMapper.findById(builder.clientId)?.fio()
                }
                                        Оплата: ${builder.amount}
                                        """, replyMarkup = replyKeyboard {
                    row { yes() }
                    row { no() }
                })
            }

        }
    }

    private suspend fun confirm(message: CommonMessage<MessageContent>, builder: PaymentBuilder): Boolean {
        val userId = message.userId()
        val text = message.text!!
        if (text == "да") {
            builder.organizationId = UserMapper.getById(userId)!!.organizationId
            builder.chatId = userId
            val payment = builder.build()
            val paymentId = PaymentMapper.insert(payment)
            sendLog(payment, paymentId, userId)
            if (PaymentMapper.paymentExists(payment.clientId)) {
                PaymentMapper.insert(
                    Payment(
                        chatId = userId,
                        clientId = payment.clientId,
                        amount = (abs(PaymentMapper.getCredit(payment.clientId)) + (payment.amount * 2)).toInt(),
                        organizationId = payment.organizationId
                    )
                )
            }
            bot.sendTextMessage(message.chat, "Сохранено")
        } else if (text == "нет") {
            builder.properties.stream()
                .filter { it.type is Photo }
                .forEach { photo ->
                    MinioService.delete(photo.value!!)
                        .onFailure {
                            runBlocking {
                                bot.sendTextMessage(
                                    message.chat,
                                    "Ошибка при удаление фотографии"
                                )
                            }
                        }
                }
            bot.sendTextMessage(message.chat, "Отменено")
        }
        return true
    }

    private suspend fun sendLog(payment: Payment, paymentId: Int, userId: Long) {
        getLogUser(userId)?.let { logId ->
            val keyboard = inlineKeyboard {
                row { dataButton("удалить", "paymentDelete-${paymentId}") }
            }
            val text = """
                #оплата
                Время: ${payment.dateTime.formatDateTime()}
                Организация: ${OrganizationMapper.getById(payment.organizationId)!!.name.hashtag()}
                Клиент: ${ClientMapper.findById(payment.clientId)?.fio().hashtag()}
                Предмет: ${ServicesMapper.getNameById(payment.serviceId!!)}
                Оплата: ${payment.amount}
                Оплатил: ${UserMapper.getById(userId)!!.name.hashtag()}
                ${payment.properties.print()}
            """.trimIndent()
            val hasPhoto = payment.properties.stream().filter { it.type is Photo }.count()
            if (hasPhoto == 1L) {
                payment.properties
                    .filter { it.type is Photo }
                    .forEach { photo ->
                        bot.sendActionUploadPhoto(logId)
                        bot.sendDocument(
                            logId,
                            InputFile.fromInput("") {
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
                bot.sendTextMessage(logId, text, replyMarkup = keyboard)
            }
        }
    }
}

suspend fun startPaymentFsm(message: CommonMessage<MessageContent>): PaymentBuilder {
    bot.sendTextMessage(message.chat, "Введите фио")
    val builder = PaymentBuilder()
    bot.sendTextMessage(message.chat, "нажмите для поиска фио", replyMarkup = switchToInlineKeyboard)
    return builder
}

