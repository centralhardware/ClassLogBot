package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.extensions.process
import me.centralhardware.znatoki.telegram.statistic.mapper.*

sealed interface ClientState : State
data class ClientInitial(override val context: Long) : ClientState
data class ClientFio(override val context: ClientBuilder, val userId: Long) : ClientState
data class ClientProperties(override val context: ClientBuilder, val userId: Long) : ClientState
data class ClientFinish(override val context: ClientBuilder, val userId: Long, val p: List<Property>) : ClientState


suspend fun BehaviourContext.buildClientFsm(
    flow: Flow<Update>,
    chatId: Long
): DefaultBehaviourContextWithFSM<ClientState> {
    val hx = buildBehaviourWithFSM(
        flow,
        onStateHandlingErrorHandler = { state, e ->
            e.printStackTrace()
            state
        }) {
        clientInitial()
        clientFio()
        clientProperty()
        clientFinish()
    }
    hx.startChain(ClientInitial(chatId))
    hx.start()
    return hx
}

private fun DefaultBehaviourContextWithFSM<ClientState>.clientInitial() {
    strictlyOn<ClientInitial> {
        sendTextMessage(it.context.toChatId(), "Введите ФИО. В формате: имя фамилия отчество.")
        ClientFio(ClientBuilder(), it.context)
    }
}

private fun DefaultBehaviourContextWithFSM<ClientState>.clientFio() {
    strictlyOn<ClientFio> {
        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        val words = contentMessage.content.asTextContent()!!.text.split(" ")
        if (words.size !in 2..3) {
            bot.sendTextMessage(
                it.userId.toChatId(),
                "Фио требуется ввести в формате: фамилия имя отчество",
            )
            it
        }
        it.context.let {
            when (words.size) {
                3 -> {
                    it.secondName = words[0]
                    it.name = words[1]
                    it.lastName = words[2]
                }
                else -> {
                    it.secondName = words[0]
                    it.name = words[1]
                    it.lastName = ""
                }
            }
        }
        if (
            ClientMapper.findAllByFio(it.context.name!!, it.context.secondName!!, it.context.lastName!!)
                .isNotEmpty()
        ) {
            bot.sendTextMessage(it.userId.toChatId(), "Данной ФИО уже содержится в базе данных")
            it
        }

        if (ConfigMapper.clientProperties().isEmpty()) {
            ClientFinish(it.context, it.userId, emptyList())
        } else {
            it.context.propertiesBuilder =
                PropertiesBuilder(ConfigMapper.clientProperties().propertyDefs.toMutableList())
            val next = it.context.nextProperty()!!
            if (next.second.isNotEmpty()) {
                send(
                    it.userId.toChatId(),
                    text = next.first,
                    replyMarkup = replyKeyboard { next.second.forEach { row { simpleButton(it) } } },
                )
            } else {
                sendTextMessage(it.userId.toChatId(), next.first)
            }
        }
        ClientProperties(it.context, it.userId)
    }
}

private fun DefaultBehaviourContextWithFSM<ClientState>.clientProperty() {
    strictlyOn<ClientProperties> {
        if (ConfigMapper.clientProperties().isEmpty()) ClientFio(ClientBuilder(), it.userId)

        val contentMessage = waitAnyContentMessage().filter { message ->
            true
        }.first()

        if (it.context.propertiesBuilder!!.process(contentMessage, bot) { properties ->
                ClientFinish(it.context, it.userId, properties)
            }) {
            null
        } else {
            it
        }

    }
}


private fun DefaultBehaviourContextWithFSM<ClientState>.clientFinish() {
    strictlyOn<ClientFinish> {
        it.context.apply {
            createdBy = it.userId
            properties = it.p
        }

        val client = it.context.build()
        client.id = ClientMapper.save(client)

        sendTextMessage(
            it.userId.toChatId(),
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!).mapNotNull {
                    ServicesMapper.getNameById(it)
                }
            ),
        )
        sendLog(client)
        bot.sendTextMessage(it.userId.toChatId(), "Создание ученика закончено")
        Trace.save("commitClient", mapOf("id" to client.id.toString()))
        null
    }
}

private suspend fun BehaviourContextWithFSM<in ClientState>.sendLog(client: Client) {
    send(
        ConfigMapper.logChat(),
        text =
            """
                #${ConfigMapper.clientName()}   
                ${
                client.getInfo(
                    ServiceMapper.getServicesForClient(client.id!!)
                        .mapNotNull { ServicesMapper.getNameById(it) })
            }
                """
                .trimIndent(),
        parseMode = MarkdownParseMode,
    )
}
