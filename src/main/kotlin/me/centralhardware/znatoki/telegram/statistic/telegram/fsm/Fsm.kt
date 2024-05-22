package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.entity.Builder
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.WrappedEvent
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.undo
import ru.nsk.kstatemachine.transition.TransitionParams

object UpdateEvent : Event

abstract class Fsm<B: Builder>(private val builder: B){

    private val stateMachine: StateMachine

    init {
        stateMachine = createFSM()
    }

    abstract fun createFSM(): StateMachine

    fun processEvent(update: Update) = runBlocking { stateMachine.processEvent(UpdateEvent, update) }

    suspend fun processState(t: TransitionParams<*>, state: DefaultState, block: (Update, B) -> Boolean){
        if (t.event is WrappedEvent) return

        val res = runCatching { process(t, block) }
            .onFailure {
                LoggerFactory.getLogger("fsm").warn("", it)
                sender().sendText("Данный тип сообщения не поддерживается или произошла ошибка", t.arg().userId(), false,)
            }
            .getOrDefault(false)

        if (!res) state.machine.undo()
    }

    fun process(t: TransitionParams<*>, block: (Update, B) -> Boolean) = block.invoke(t.arg(), builder)

    fun removeFromStorage(t: TransitionParams<*>) = storage().remove(t.arg().userId())

}


val fsmLog = StateMachine.Logger { lazyMessage ->
    LoggerFactory.getLogger("fsm").info(lazyMessage())
}

fun mapError(chatId: Long): (String) -> Unit = { error -> sender().sendText(error, chatId, false) }

fun getLogUser(userId: Long): Long? =
    userMapper().getById(userId)?.organizationId?.let { user ->
        organizationMapper().getById(user)?.logChatId
    }

fun TransitionParams<*>.arg() = this.argument as Update