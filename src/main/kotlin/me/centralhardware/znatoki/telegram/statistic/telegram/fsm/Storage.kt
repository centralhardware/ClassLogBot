package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.entity.*
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.StateMachine

@Component
class Storage {

    private val fsms: MutableMap<Long, Fsm<Builder>> = mutableMapOf()

    fun <B: Builder> create(chatId: Long, fsm: Fsm<B>){
        fsms[chatId] = fsm as Fsm<Builder>
    }

    fun process(chatId: Long, update: Update) =
        fsms[chatId]?.let {
            runBlocking { it.processEvent(update) }
        }

    fun remove(chatId: Long) = fsms.remove(chatId)

    fun contain(chatId: Long): Boolean = fsms.containsKey(chatId)

}