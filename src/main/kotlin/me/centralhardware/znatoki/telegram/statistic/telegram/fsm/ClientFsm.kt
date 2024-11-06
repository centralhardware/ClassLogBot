package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.mapper.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine

sealed class ClientState : DefaultState() {
    object Initial : ClientState()

    object Fio : ClientState()

    object Properties : ClientState()

    object Finish : ClientState(), FinalState
}

class ClientFsm(builder: ClientBuilder) : Fsm<ClientBuilder>(builder) {
    override fun createFSM(): StateMachine =
        createStdLibStateMachine(
            "client",
            creationArguments = buildCreationArguments { isUndoEnabled = true },
        ) {
            logger = fsmLog
            addInitialState(ClientState.Initial) {
                transition<UpdateEvent> { targetState = ClientState.Fio }
            }
            addState(ClientState.Fio) {
                transition<UpdateEvent> { targetState = ClientState.Properties }
                onEntry { processState(it, this, ::fio) }
            }
            addState(ClientState.Properties) {
                transition<UpdateEvent> { targetState = ClientState.Finish }
                onEntry { processState(it, this, ::property) }
            }
            addState(ClientState.Finish)
            onFinished { removeFromStorage(it) }
        }

    private suspend fun fio(
        message: CommonMessage<MessageContent>,
        builder: ClientBuilder,
    ): Boolean {
        val chatId = message.userId()
        val telegramUser = UserMapper.findById(chatId)!!
        val words = message.content.asTextContent()!!.text.split(" ")
        if (words.size !in 2..3) {
            bot.sendTextMessage(
                message.chat,
                "Фио требуется ввести в формате: фамилия имя отчество",
            )
            return false
        }
        builder.let {
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
            ClientMapper.findAllByFio(builder.name!!, builder.secondName!!, builder.lastName!!)
                .isNotEmpty()
        ) {
            bot.sendTextMessage(message.chat, "Данной ФИО уже содержится в базе данных")
            return false
        }

        if (ConfigMapper.clientProperties().isEmpty()) {
            finish(listOf(), message, builder)
        } else {
            builder.propertiesBuilder =
                PropertiesBuilder(ConfigMapper.clientProperties().propertyDefs.toMutableList())
            val next = builder.nextProperty()!!
            if (next.second.isNotEmpty()) {
                bot.send(
                    message.chat,
                    text = next.first,
                    replyMarkup = replyKeyboard { next.second.forEach { row { simpleButton(it) } } },
                )
            } else {
                bot.sendTextMessage(message.chat, next.first)
            }
        }
        return true
    }

    suspend fun property(message: CommonMessage<MessageContent>, builder: ClientBuilder): Boolean {
        if (ConfigMapper.clientProperties().isEmpty()) return true

        return builder.propertiesBuilder!!.process(message) {
            runBlocking { finish(it, message, builder) }
        }
    }

    private suspend fun finish(
        p: List<Property>,
        message: CommonMessage<MessageContent>,
        builder: ClientBuilder,
    ) {
        builder.apply {
            createdBy = message.userId()
            properties = p
        }

        val client = builder.build()
        client.id = ClientMapper.save(client)

        bot.sendTextMessage(
            message.chat,
            client.getInfo(
                ServiceMapper.getServicesForClient(client.id!!).mapNotNull {
                    ServicesMapper.getNameById(it)
                }
            ),
        )
        sendLog(client, message.userId())
        bot.sendTextMessage(message.chat, "Создание ученика закончено")
        Trace.save("commitClient", mapOf("id" to client.id.toString()))
    }

    private suspend fun sendLog(client: Client, chatId: Long) {
        bot.send(
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
}

suspend fun startClientFsm(message: CommonMessage<MessageContent>): ClientBuilder {
    bot.sendTextMessage(message.chat, "Введите ФИО. В формате: имя фамилия отчество.")
    return ClientBuilder()
}
