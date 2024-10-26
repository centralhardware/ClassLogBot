package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.userId

object Storage {

    private val fsms: MutableMap<Long, Fsm<Builder>> = mutableMapOf()

    fun <B : Builder> create(chatId: Long, fsm: Fsm<B>) {
        fsms[chatId] = fsm as Fsm<Builder>
    }

    fun process(message: CommonMessage<MessageContent>) =
        fsms[message.userId()]?.let { runBlocking { it.processEvent(message) } }

    fun remove(chatId: Long) = fsms.remove(chatId)

    fun contain(chatId: Long): Boolean = fsms.containsKey(chatId)
}
