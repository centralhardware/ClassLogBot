package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import ru.nsk.kstatemachine.statemachine.StateMachine

object Storage {

    private val fsms: MutableMap<Long, StateMachine> = mutableMapOf()

    fun create(chatId: Long, fsm: StateMachine) {
        fsms[chatId] = fsm
    }

    suspend fun process(message: CommonMessage<MessageContent>) =
        fsms[message.userId()]?.processEvent(TelegramEvent(message))

    fun remove(chatId: Long) = fsms.remove(chatId)

    fun contain(chatId: Long): Boolean = fsms.containsKey(chatId)
}
