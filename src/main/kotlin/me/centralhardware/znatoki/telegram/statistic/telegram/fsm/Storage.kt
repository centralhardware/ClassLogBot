package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.entity.*
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.StateMachine

@Component
class Storage {

    private val fsm: MutableMap<Long, Pair<StateMachine, Builder>> = mutableMapOf()

    fun create(chatId: Long, stateMachine: StateMachine, builder: Builder){
        fsm[chatId] = Pair(stateMachine, builder)
    }

    fun process(chatId: Long, update: Update) =
        fsm[chatId]?.let {
            runBlocking { it.first.processEvent(UpdateEvent.UpdateEvent, Pair(update, it.second)) }
        }

    fun remove(chatId: Long) = fsm.remove(chatId)

    fun contain(chatId: Long): Boolean = fsm.containsKey(chatId)

}