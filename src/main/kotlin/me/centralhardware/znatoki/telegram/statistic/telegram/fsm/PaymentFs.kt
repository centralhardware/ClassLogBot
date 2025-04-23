package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.utils.asTextedInput
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.extensions.process
import me.centralhardware.znatoki.telegram.statistic.extensions.switchToInlineKeyboard
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.extensions.yesNoKeyboard
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.MinioService

sealed interface PaymentState : State
data class PaymentInitial(override val context: Long) : PaymentState
data class PaymentPupil(override val context: PaymentBuilder, val userId: Long) : PaymentState
data class PaymentService(override val context: PaymentBuilder, val userId: Long) : PaymentState
data class PaymentAmount(override val context: PaymentBuilder, val userId: Long) : PaymentState
data class PaymentProperty(override val context: PaymentBuilder, val userId: Long) : PaymentState
data class PaymentConfirm(override val context: PaymentBuilder, val userId: Long) : PaymentState

suspend fun BehaviourContext.buildPaymentFsm(
    flow: Flow<Update>,
    chatId: Long
): DefaultBehaviourContextWithFSM<PaymentState> {
    val hx = buildBehaviourWithFSM(
        flow,
        onStateHandlingErrorHandler = { state, e ->
            e.printStackTrace()
            state
        }) {
        paymentInitial()
        paymentPupil()
        paymentService()
        paymentAmount()
        paymentProperty()
        paymentConfirm()
    }
    hx.startChain(PaymentInitial(chatId))
    hx.start()
    return hx
}

private fun DefaultBehaviourContextWithFSM<PaymentState>.paymentInitial() {
    strictlyOn<PaymentInitial> {
        val builder = PaymentBuilder()
        builder.chatId = it.context
        sendTextMessage(
            it.context.toChatId(),
            "Введите фио. \nнажмите для поиска фио",
            replyMarkup = switchToInlineKeyboard,
        )
        PaymentPupil(builder, it.context)
    }
}

private fun DefaultBehaviourContextWithFSM<PaymentState>.paymentPupil() {
    strictlyOn<PaymentPupil> {
        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        val userId = contentMessage.userId()
        validateFio(contentMessage.content.asTextedInput()!!.text!!)
            .mapLeft(mapError(contentMessage))
            .map { fio ->
                val id = fio.split(" ")[0].toInt()

                it.context.clientId = id
                UserMapper.findById(userId)?.services?.let { services ->
                    bot.sendTextMessage(
                        contentMessage.chat,
                        "Выберите предмет",
                        replyMarkup =
                            replyKeyboard {
                                services.forEach {
                                    row {
                                        ServicesMapper.getNameById(it)?.let { name ->
                                            simpleButton(name)
                                        }
                                    }
                                }
                            },
                    )
                }
            }
        PaymentService(it.context, it.userId)
    }
}

private fun DefaultBehaviourContextWithFSM<PaymentState>.paymentService() {
    strictlyOn<PaymentService> {
        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        validateService(contentMessage.content.asTextedInput()!!.text!!)
            .mapLeft(mapError(contentMessage))
            .map { service ->
                it.context.serviceId = ServicesMapper.getServiceId(service)!!

                bot.sendTextMessage(
                    contentMessage.chat,
                    "Введите сумму оплаты",
                    replyMarkup = ReplyKeyboardRemove(),
                )
            }
        PaymentAmount(it.context, it.userId)
    }
}

private fun DefaultBehaviourContextWithFSM<PaymentState>.paymentAmount() {
    strictlyOn<PaymentAmount> {
        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        validateAmount(contentMessage.content.asTextedInput()!!.text!!)
            .mapLeft(mapError(contentMessage))
            .map { amount ->
                it.context.amount = amount
                if (ConfigMapper.paymentProperties().isEmpty()) {
                    bot.sendTextMessage(
                        contentMessage.chat,
                        it.context.let {
                            """
                                        ФИО: ${
                            ClientMapper.findById(it.clientId!!)?.fio()
                        }
                                        Оплата: ${it.amount}
                                        """
                        },
                        replyMarkup = yesNoKeyboard,
                    )
                    PaymentConfirm(it.context, it.userId)
                } else {
                    it.context.propertiesBuilder =
                        PropertiesBuilder(
                            ConfigMapper.paymentProperties().propertyDefs.toMutableList()
                        )
                    val next = it.context.nextProperty()!!
                    if (next.second.isNotEmpty()) {
                        bot.sendTextMessage(
                            contentMessage.chat,
                            next.first,
                            replyMarkup =
                                replyKeyboard { next.second.forEach { row { simpleButton(it) } } },
                        )
                    } else {
                        bot.sendTextMessage(contentMessage.chat, next.first)
                    }
                    PaymentProperty(it.context, it.userId)
                }
            }
        it
    }
}

private fun DefaultBehaviourContextWithFSM<PaymentState>.paymentProperty() {
    strictlyOn<PaymentProperty> {
        if (ConfigMapper.paymentProperties().isEmpty()) return@strictlyOn PaymentConfirm(it.context, it.userId)

        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        if (it.context.propertiesBuilder!!.process(contentMessage, bot) { properties ->
                it.context.properties = properties
                runBlocking {
                    bot.sendTextMessage(
                        contentMessage.chat,
                        """
                                        ФИО: ${
                            ClientMapper.findById(it.context.clientId!!)?.fio()
                        }
                                        Оплата: ${it.context.amount}
                                        """,
                        replyMarkup = yesNoKeyboard,
                    )
                }
            }) {
            PaymentConfirm(it.context, it.userId)
        } else {
            it
        }
    }
}

private fun DefaultBehaviourContextWithFSM<PaymentState>.paymentConfirm() {
    strictlyOn<PaymentConfirm> {
        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        val userId = contentMessage.userId()
        when (contentMessage.text!!) {
            "да" -> {
                it.context.chatId = userId
                val payment = it.context.build()
                val paymentId = PaymentMapper.insert(payment)
                sendLog(payment, paymentId, userId)
                bot.sendTextMessage(contentMessage.chat, "Сохранено", replyMarkup = ReplyKeyboardRemove())
                Trace.save("commitPayment", mapOf("id" to paymentId.toString()))
                Storage.remove(userId)
                null
            }
            "нет" -> {
                it.context.properties!!
                    .stream()
                    .filter { it.type is Photo }
                    .forEach { photo ->
                        MinioService.delete(photo.value!!).onFailure {
                            runBlocking {
                                bot.sendTextMessage(contentMessage.chat, "Ошибка при удаление фотографии")
                            }
                        }
                    }
                bot.sendTextMessage(contentMessage.chat, "Отменено", replyMarkup = ReplyKeyboardRemove())
                Trace.save("rollbackPayment", mapOf())
                Storage.remove(userId)
                null
            }

            else -> it
        }
    }
}

private suspend fun DefaultBehaviourContextWithFSM<PaymentState>.sendLog(
    payment: Payment,
    paymentId: Int,
    userId: Long
) {
    val keyboard = inlineKeyboard {
        row { dataButton("Удалить", "paymentDelete-${paymentId}") }
    }
    val text =
        """
            #оплата
            Время: ${payment.dateTime.formatDateTime()}
            Клиент: ${ClientMapper.findById(payment.clientId)?.fio().hashtag()}
            Предмет: ${ServicesMapper.getNameById(payment.serviceId)}
            Оплата: ${payment.amount}
            Оплатил: ${UserMapper.findById(userId)!!.name.hashtag()}
            ${payment.properties.print()}
        """
            .trimIndent()
    val hasPhoto = payment.properties.stream().filter { it.type is Photo }.count()
    if (hasPhoto == 1L) {
        sendActionUploadPhoto(ConfigMapper.logChat())
        payment.properties
            .filter { it.type is Photo }
            .forEach { photo ->
                sendActionUploadPhoto(ConfigMapper.logChat())
                sendPhoto(
                    ConfigMapper.logChat(),
                    InputFile.fromInput("") {
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
                    text = text,
                )
            }
    } else {
        sendTextMessage(ConfigMapper.logChat(), text, replyMarkup = keyboard)
    }
}

