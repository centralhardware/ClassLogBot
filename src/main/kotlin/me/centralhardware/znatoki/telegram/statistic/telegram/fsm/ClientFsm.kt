package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.StateMachine.CreationArguments
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine


sealed class ClientState : DefaultState() {
    object Initial : ClientState()
    object Fio : ClientState()
    object Properties : ClientState()
    object Finish : ClientState(), FinalState
}

class ClientFsm(builder: ClientBuilder) : Fsm<ClientBuilder>(builder) {
    override fun createFSM(): StateMachine = createStdLibStateMachine("client", creationArguments = CreationArguments(isUndoEnabled = true)) {
        logger = fsmLog
        addInitialState(ClientState.Initial) {
            transition<UpdateEvent> {
                targetState = ClientState.Fio
            }
        }
        addState(ClientState.Fio) {
            transition<UpdateEvent> {
                targetState = ClientState.Properties
            }
            onEntry { processState(it, this, ::fio) }
        }
        addState(ClientState.Properties) {
            transition<UpdateEvent> {
                targetState = ClientState.Finish
            }
            onEntry { processState(it, this, ::property) }
        }
        addState(ClientState.Finish)
        onFinished { removeFromStorage(it) }
    }

    fun fio(update: Update, builder: ClientBuilder): Boolean {
        val chatId = update.userId()
        val telegramUser = userMapper().getById(chatId)!!
        val words = update.message.text.split(" ")
        if (words.size !in 2..3) {
            sender().sendMessageFromResource(I18n.Message.INPUT_FIO_REQUIRED_FORMAT, chatId)
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
        if (clientService().checkExistenceByFio(builder.name, builder.secondName, builder.lastName)) {
            sender().sendMessageFromResource(I18n.Message.FIO_ALREADY_IN_DATABASE, chatId)
            return false
        }

        val org = organizationMapper().getById(telegramUser.organizationId)!!

        if (org.clientCustomProperties.isEmpty()) {
            finish(listOf(), chatId, builder)
        } else {
            builder.propertiesBuilder = PropertiesBuilder(org.clientCustomProperties.propertyDefs.toMutableList())
            val next = builder.nextProperty()!!
            if (next.second.isNotEmpty()) {
                sender().send {
                    execute(replyKeyboard {
                        chatId(chatId)
                        text(next.first)
                        next.second.forEach { row { btn(it) } }
                    }.build())
                }
            } else {
                sender().sendText(next.first, chatId)
            }
        }
        return true
    }

    fun property(update: Update, builder: ClientBuilder): Boolean {
        val telegramUser = userMapper().getById(update.userId())!!

        if (organizationMapper().getById(telegramUser.organizationId)!!.clientCustomProperties.isEmpty()) return true

        return builder.propertiesBuilder.process(update){ finish(it, update.userId(), builder) }
    }

    fun finish(p: List<Property>, chatId: Long, builder: ClientBuilder) {
        builder.apply {
            organizationId = userMapper().getById(chatId)!!.organizationId
            createdBy = chatId
            properties = p
        }

        val client = builder.build()
        clientService().save(client)

        sender().sendMessageWithMarkdown(
            client.getInfo(
                serviceMapper().getServicesForCLient(client.id!!)
                    .mapNotNull { servicesMapper().getNameById(it) }), chatId
        )
        sendLog(client, chatId)
        sender().sendMessageFromResource(I18n.Message.CREATE_PUPIL_FINISHED, chatId)
    }

    fun sendLog(client: Client, chatId: Long) {
        getLogUser(chatId)?.let { logId ->
            val message = SendMessage.builder()
                .text(
                    """
                #${organizationMapper().getById(client.organizationId)!!.clientName}   
                ${
                        client.getInfo(
                            serviceMapper().getServicesForCLient(client.id!!)
                                .mapNotNull { servicesMapper().getNameById(it) })
                    }
                """.trimIndent()
                )
                .chatId(logId)
                .parseMode("Markdown")
                .build()
            sender().send { execute(message) }
        }
    }

}

fun startClientFsm(update: Update): ClientBuilder {
    sender().sendMessageAndRemoveKeyboard(
        resourceBundle().getString("INPUT_FIO_IN_FORMAT"),
        update.userId()
    )
    return ClientBuilder()
}