package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlin.reflect.KSuspendFunction2
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.entity.Builder
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.WrappedEvent
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.undo
import ru.nsk.kstatemachine.transition.TransitionParams

object UpdateEvent : Event

abstract class Fsm<B : Builder>(private val builder: B, val bot: TelegramBot) {

    private val stateMachine: StateMachine

    init {
        stateMachine = createFSM()
    }

    abstract fun createFSM(): StateMachine

    suspend fun processEvent(message: CommonMessage<MessageContent>) = stateMachine.processEvent(UpdateEvent, message)

    suspend fun processState(
        t: TransitionParams<*>,
        state: DefaultState,
        block: KSuspendFunction2<CommonMessage<MessageContent>, B, Boolean>,
    ) {
        if (t.event is WrappedEvent) return

        val res =
            runCatching { process(t, block) }
                .onFailure {
                    KSLog.warning("", it)
                    bot.sendTextMessage(
                        t.arg().chat,
                        "Данный тип сообщения не поддерживается или произошла ошибка",
                    )
                }
                .getOrDefault(false)

        if (!res) state.machine.undo()
    }

    suspend fun process(
        t: TransitionParams<*>,
        block: KSuspendFunction2<CommonMessage<MessageContent>, B, Boolean>,
    ) = block.invoke(t.arg(), builder)

    fun removeFromStorage(t: TransitionParams<*>) = Storage.remove(t.arg().userId())

    fun mapError(message: CommonMessage<MessageContent>): (String) -> Unit = { error ->
        runBlocking { bot.sendTextMessage(message.chat, error) }
    }
}

val fsmLog = StateMachine.Logger { lazyMessage -> KSLog.info(lazyMessage()) }

fun TransitionParams<*>.arg() = this.argument as CommonMessage<MessageContent>
