package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import me.centralhardware.znatoki.telegram.statistic.userId
import me.centralhardware.znatoki.telegram.statistic.utils.*
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.*

sealed class ClientState : DefaultState() {
    object Initial : ClientState()
    object Fio : ClientState()
    object Properties : ClientState()
}

fun createClientFsm() = createStdLibStateMachine("client", enableUndo = true) {
    logger = StateMachine.Logger { lazyMessage ->
        LoggerFactory.getLogger("fsm").info(lazyMessage())
    }
    addInitialState(ClientState.Initial) {
        transition<UpdateEvent.UpdateEvent> {
            targetState = ClientState.Fio
        }
    }
    addState(ClientState.Fio){
        transition<UpdateEvent.UpdateEvent> {
            targetState = ClientState.Fio
        }
        onEntry {
            val res = fio(it.argClient().first, it.argClient().second)
            if (!res) machine.undo()
        }
    }
    addState(ClientState.Properties){
        transition<UpdateEvent.UpdateEvent> {
            targetState = ClientState.Fio
        }
        onEntry {
            val res = fio(it.argClient().first, it.argClient().second)
            if (!res) machine.undo()
        }
    }
}

fun startClientFsm(update: Update): ServiceBuilder{
    sender().sendMessageAndRemoveKeyboard(
        resourceBundle().getString("INPUT_FIO_IN_FORMAT"),
        update.userId()
    )
    return ServiceBuilder()
}


fun fio(update: Update, builder: ClientBuilder): Boolean{
    val chatId = update.userId()
    val telegramUser = userMapper().getById(chatId)!!
    val words = update.message.text.split(" ")
    if (words.size !in 2..3) {
        sender().sendMessageFromResource(I18n.Message.INPUT_FIO_REQUIRED_FORMAT, chatId)
        return false
    }
    builder.let{
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
        return true
    } else {
        builder.propertiesBuilder = PropertiesBuilder(org.clientCustomProperties.propertyDefs.toMutableList())
        val next = builder.nextProperty()!!
        if (next.second.isNotEmpty()) {
            sender().send(replyKeyboard {
                chatId(chatId)
                text(next.first)
                next.second.forEach { row { btn(it) } }
            })
        } else {
            sender().sendText(next.first, chatId)
        }
        return false
    }

}

fun property(update: Update, builder: ClientBuilder): Boolean {
    val chatId = update.userId()
    val telegramUser = userMapper().getById(update.userId())!!
    val org = organizationMapper().getById(telegramUser.organizationId)!!

    if (org.clientCustomProperties.isEmpty()) return true

    var isFinished = false
    processCustomProperties(update, builder.propertiesBuilder) {
        finish(it, chatId, builder)
        isFinished = true
    }
    return isFinished
}

fun finish(p: List<Property>, chatId: Long, builder: ClientBuilder){
    builder.let{
        it.organizationId = userMapper().getById(chatId)!!.organizationId
        it.created_by = chatId
        if (it.properties.isNotEmpty()) it.properties = p
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
                        ${client.getInfo(serviceMapper().getServicesForCLient(client.id!!).mapNotNull { servicesMapper().getNameById(it) })}
                        """.trimIndent()
            )
            .chatId(logId)
            .parseMode("Markdown")
            .build()
        sender().send(message)
    }
}