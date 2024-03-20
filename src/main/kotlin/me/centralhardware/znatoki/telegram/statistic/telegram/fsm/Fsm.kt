package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.entity.Builder
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.*

sealed interface UpdateEvent {
    object UpdateEvent : Event
}

val fsmLog = StateMachine.Logger { lazyMessage ->
    LoggerFactory.getLogger("fsm").info(lazyMessage())
}

fun mapError(chatId: Long): (String) -> Unit = { error -> sender().sendText(error, chatId, false) }

fun getLogUser(userId: Long): Long? =
    userMapper().getById(userId)?.organizationId?.let { user ->
        organizationMapper().getById(user)?.logChatId
    }

fun processCustomProperties(
    update: Update,
    builder: PropertiesBuilder,
    onFinish: (List<Property>) -> Unit
): Boolean {
    val chatId = update.userId()

    var isFinished = false
    builder.validate(update)
        .mapLeft { error ->
            sender().sendText(error, chatId)
        }
        .map {
            builder.setProperty(update)

            builder.next()?.let { next ->
                if (next.second.isNotEmpty()) {
                    sender().send{
                        execute(replyKeyboard {
                            text(next.first)
                            chatId(chatId)
                            next.second.forEach {
                                row { btn(it) }
                            }
                        }.build())
                    }
                } else {
                    sender().sendText(next.first, chatId)
                }
            } ?: run {
                onFinish(builder.properties)
                isFinished = true
            }
        }
    return isFinished
}

suspend fun <B: Builder> processState(t: TransitionParams<*>, state: DefaultState, block: (Update, B) -> Boolean){
    if (t.event is WrappedEvent) return

    val res = runCatching { process(t, block) }
        .onFailure {
            LoggerFactory.getLogger("fsm").warn("", it)
            sender().sendText("Данный тип сообщения не поддерживается или произошла ошибка", t.getArg<B>().first.userId(), false,)
        }
        .getOrDefault(false)

    if (!res) state.machine.undo()
}


fun <B: Builder> process(t: TransitionParams<*>, block: (Update, B) -> Boolean): Boolean{
    val argument = t.getArg<B>()
    return block.invoke(argument.first, argument.second)
}

fun <B: Builder> removeFromStorage(t: TransitionParams<*>){
    storage().remove(t.getArg<B>().first.userId())
}

fun <B: Builder> TransitionParams<*>.getArg() = argument as Pair<Update, B>