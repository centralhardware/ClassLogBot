package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.stop
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.entity.Builder
import me.centralhardware.znatoki.telegram.statistic.extensions.userId

object Storage {

    private val fsms: MutableMap<Long, DefaultBehaviourContextWithFSM<*>> = mutableMapOf()

    fun <B : State> create(chatId: Long, fsm: DefaultBehaviourContextWithFSM<B>) {
        fsms[chatId] = fsm
    }

    fun remove(chatId: Long) {
        fsms[chatId]?.stop()
        fsms.remove(chatId)
    }

    fun contain(chatId: Long): Boolean = fsms.containsKey(chatId)
}
