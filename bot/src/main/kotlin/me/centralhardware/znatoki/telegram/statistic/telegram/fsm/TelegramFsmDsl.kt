package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

/**
 * DSL for building Telegram bot finite state machines (FSM).
 * Allows declarative definition of multi-step user interactions with validation,
 * custom keyboards, and state transitions.
 *
 * Example:
 * ```
 * telegramFsm("example", ctx, userId) {
 *     text(prompt = "Enter name") { ctx, value -> ctx.name = value }
 *     confirm(prompt = { "Save ${it.name}?" }, onSave = { save(it) }, onCancel = {})
 * }
 * ```
 */

import me.centralhardware.znatoki.telegram.statistic.telegram.InlineSearchType
import arrow.core.Either
import arrow.core.combine
import arrow.core.flatMap
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineQueryInCurrentChatButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.validateDate
import me.centralhardware.znatoki.telegram.statistic.validateEnum
import me.centralhardware.znatoki.telegram.statistic.validateInt
import me.centralhardware.znatoki.telegram.statistic.validatePhoto
import me.centralhardware.znatoki.telegram.statistic.validateTelephone
import me.centralhardware.znatoki.telegram.statistic.validateText
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.onTriggered
import java.time.LocalDate

const val SKIP = "/skip"
const val COMPLETE = "/complete"

val yesNoKeyboard = replyKeyboard {
    row { simpleButton("да") }
    row { simpleButton("нет") }
}

val switchToInlineKeyboard = inlineKeyboard { row { inlineQueryInCurrentChatButton("inline", "") } }

fun switchToInlineKeyboard(searchType: String) = inlineKeyboard {
    row { inlineQueryInCurrentChatButton("Поиск", "$searchType ") }
}


class TelegramEvent(val message: CommonMessage<*>) : Event
class Initial() : Event
typealias FioValue = Triple<String, String, String>

val fsmLog = StateMachine.Logger { lazyMessage -> KSLog.info(lazyMessage()) }

class FsmBuilder<CTX : Any>(
    private val name: String,
    private val ctx: CTX,
) {
    internal val steps = mutableListOf<Step<CTX, *>>()
    internal var onFinish: (suspend (CommonMessage<MessageContent>, CTX) -> Unit) = { _, _ -> }

    fun onFinish(block: suspend (CommonMessage<MessageContent>, CTX) -> Unit) {
        onFinish = block
    }

    fun text(
        prompt: String,
        inline: Boolean = false,
        inlineSearchType: InlineSearchType = InlineSearchType.STUDENT,
        optionalSkip: Boolean = false,
        validator: ((String) -> Either<String, Any>) = { _ -> Either.Right(Unit) },
        apply: (ctx: CTX, value: String) -> Unit
    ) {
        steps += Step.TextStep(prompt, steps.size, inline, inlineSearchType, optionalSkip, validator, apply)
    }

    fun int(
        prompt: String,
        optionalSkip: Boolean = false,
        validator: ((Int?) -> Either<String, Unit>) = { _ -> Either.Right(Unit) },
        apply: (ctx: CTX, value: Int) -> Unit
    ) {
        steps += Step.IntStep(prompt, steps.size, optionalSkip, validator, apply)
    }

    fun date(
        prompt: String,
        optionalSkip: Boolean = false,
        apply: (ctx: CTX, value: LocalDate) -> Unit
    ) {
        steps += Step.DateStep(prompt, steps.size, optionalSkip, apply)
    }

    fun phone(
        prompt: String,
        optionalSkip: Boolean = false,
        apply: (ctx: CTX, value: String) -> Unit
    ) {
        steps += Step.PhoneStep(prompt, steps.size, optionalSkip, apply)
    }

    fun enum(
        prompt: String,
        options: List<String>,
        optionalSkip: Boolean = false,
        apply: (ctx: CTX, value: String) -> Unit
    ) {
        steps += Step.EnumStep(prompt, steps.size, options, optionalSkip, apply)
    }

    fun fio(
        prompt: String,
        optionalSkip: Boolean = false,
        duplicateCheck: (suspend (FioValue) -> Boolean)? = null,
        apply: (ctx: CTX, value: FioValue) -> Unit
    ) {
        steps += Step.FioStep(
            prompt = prompt,
            steps.size,
            skip = optionalSkip,
            duplicateCheck = duplicateCheck,
            apply = apply
        )
    }

    fun photo(
        prompt: String,
        optionalSkip: Boolean = false,
        apply: (ctx: CTX, value: String) -> Unit
    ) {
        steps += Step.PhotoStep(
            prompt = prompt,
            steps.size,
            skip = optionalSkip,
            apply = apply
        )
    }

    fun confirm(
        prompt: (CTX) -> String,
        onSave: suspend (CTX) -> Unit,
        onCancel: suspend (CTX) -> Unit
    ) {
        steps += Step.ConfirmStep(
            promptFormatter = prompt,
            steps.size,
            onSave = onSave,
            onCancel = onCancel
        )
    }

    fun <T> multi(
        prompt: String,
        inline: Boolean = false,
        maxCount: Int = Int.MAX_VALUE,
        parse: (String) -> Either<String, T>,
        apply: (ctx: CTX, value: Set<T>) -> Unit
    ) {
        steps += Step.MultiStep(
            prompt = prompt,
            inline,
            steps.size,
            maxCount = maxCount,
            parse = parse,
            apply = apply
        )
    }

    internal fun build(): Built<CTX> = Built(name, ctx, steps, onFinish)
}

internal data class Built<CTX : Any>(
    val name: String,
    val ctx: CTX,
    val steps: List<Step<CTX, *>>,
    val onFinish: (suspend (CommonMessage<MessageContent>, CTX) -> Unit),
)

sealed class Step<CTX : Any, T>(
    open val prompt: String,
    open val index: Int,
    open val apply: (CTX, T) -> Unit
) : DefaultState() {

    data class TextStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val inline: Boolean,
        val inlineSearchType: InlineSearchType,
        val skip: Boolean,
        val validator: (String) -> Either<String, Any>,
        override val apply: (CTX, String) -> Unit
    ) : Step<CTX, String>(prompt, index, apply)

    data class IntStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val skip: Boolean,
        val validator: (Int?) -> Either<String, Unit>,
        override val apply: (CTX, Int) -> Unit
    ) : Step<CTX, Int>(prompt, index, apply)

    data class DateStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val skip: Boolean,
        override val apply: (CTX, LocalDate) -> Unit
    ) : Step<CTX, LocalDate>(prompt, index, apply)

    data class PhoneStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val skip: Boolean,
        override val apply: (CTX, String) -> Unit
    ) : Step<CTX, String>(prompt, index, apply)

    data class EnumStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val options: List<String>,
        val skip: Boolean,
        override val apply: (CTX, String) -> Unit
    ) : Step<CTX, String>(prompt, index, apply)

    data class FioStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val skip: Boolean = false,
        val duplicateCheck: (suspend (FioValue) -> Boolean)?,
        override val apply: (CTX, FioValue) -> Unit
    ) : Step<CTX, FioValue>(prompt, index, apply)

    data class PhotoStep<CTX : Any>(
        override val prompt: String,
        override val index: Int,
        val skip: Boolean = false,
        override val apply: (CTX, String) -> Unit
    ) : Step<CTX, String>(prompt, index, apply)

    data class ConfirmStep<CTX : Any>(
        val promptFormatter: (CTX) -> String,
        override val index: Int,
        val onSave: suspend (CTX) -> Unit,
        val onCancel: suspend (CTX) -> Unit
    ) : Step<CTX, Unit>("", index, { _, _ -> })

    data class MultiStep<CTX : Any, T>(
        override val prompt: String,
        val inline: Boolean,
        override val index: Int,
        val maxCount: Int,
        val parse: (String) -> Either<String, T>,
        override val apply: (CTX, @UnsafeVariance Set<T>) -> Unit
    ) : Step<CTX, Set<T>>(prompt, index, apply)
}

suspend fun <CTX : Any> BehaviourContext.telegramFsm(
    name: String,
    ctx: CTX,
    build: FsmBuilder<CTX>.() -> Unit,
    userId: Long
): StateMachine {
    val builder = FsmBuilder(name, ctx).apply(build)
    val built = builder.build()

    return createStateMachine(
        CoroutineScope(newSingleThreadContext("single threaded context")),
        creationArguments = buildCreationArguments { isUndoEnabled = true },
    ) {
        logger = fsmLog

        val finish = object : DefaultState(), FinalState {}
        addState(finish)

        val states: List<State> = built.steps.mapIndexed { i, it ->
            if (i == 0) {
                addInitialState(it)
            } else {
                addState(it)
            }
        }


        // для каждого шага поведение
        states.forEachIndexed { idx, state ->
            val step = built.steps[idx]
            val next: State = if (idx == states.lastIndex) finish else states[idx + 1]

            state.onEntry { params ->
                when (step) {
                    is Step.TextStep -> {
                        if (step.inline) {
                            val prefix = when (step.inlineSearchType) {
                                InlineSearchType.TUTOR -> "t:"
                                InlineSearchType.STUDENT -> "s:"
                            }
                            sendTextMessage(
                                userId.toChatId(),
                                step.prompt,
                                replyMarkup = switchToInlineKeyboard(prefix)
                            )
                        } else {
                            sendTextMessage(userId.toChatId(), step.prompt, replyMarkup = ReplyKeyboardRemove())
                        }
                    }

                    is Step.MultiStep<*, *> -> {
                        if (step.inline) {
                            sendTextMessage(userId.toChatId(), step.prompt, replyMarkup = switchToInlineKeyboard)
                        } else {
                            sendTextMessage(userId.toChatId(), step.prompt, replyMarkup = ReplyKeyboardRemove())
                        }
                    }

                    is Step.EnumStep -> send(
                        userId.toChatId(),
                        text = step.prompt,
                        replyMarkup = replyKeyboard {
                            step.options.forEach { row { simpleButton(it) } }
                        }
                    )

                    is Step.ConfirmStep -> sendTextMessage(
                        userId.toChatId(),
                        step.promptFormatter.invoke(ctx),
                        replyMarkup = yesNoKeyboard
                    )

                    else -> sendTextMessage(userId.toChatId(), step.prompt, replyMarkup = ReplyKeyboardRemove())
                }
            }

            var memo: Any? = null

            state.transition<TelegramEvent> {
                guard = { e: EventAndArgument<TelegramEvent> ->
                    val message = e.event.message
                    val text = message.textOrNull()

                    fun reply(err: String) {
                        runBlocking { sendTextMessage(userId.toChatId(), err) }
                    }

                    when (step) {

                        is Step.TextStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                message.validateText()
                                    .combine(step.validator.invoke(text!!), { f, s -> "$f $s" }, { f, _ -> f })
                                    .mapLeft { reply(it) }
                                    .map { memo = it }
                                    .isRight()
                            }
                        }

                        is Step.IntStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                message.validateInt()
                                    .flatMap { validInt ->
                                        step.validator.invoke(text?.toIntOrNull())
                                    }
                                    .mapLeft { reply(it) }
                                    .map { memo = text!!.toInt() }
                                    .isRight()
                            }
                        }

                        is Step.DateStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                message.validateDate()
                                    .mapLeft { reply(it) }
                                    .map { memo = it }
                                    .isRight()
                            }
                        }

                        is Step.PhoneStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                message.validateTelephone()
                                    .mapLeft { reply(it) }
                                    .map { memo = it }
                                    .isRight()
                            }
                        }

                        is Step.EnumStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                message.validateEnum(step.options)
                                    .mapLeft { reply(it) }
                                    .map { memo = it }
                                    .isRight()
                            }
                        }

                        is Step.FioStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                val raw = text?.trim().orEmpty()
                                val words = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
                                val okFormat = words.size in 2..3
                                if (!okFormat) {
                                    reply("ФИО требуется ввести в формате: фамилия имя [отчество]")
                                    false
                                } else {
                                    val fio = if (words.size == 3) {
                                        Triple(words[0], words[1], words[2])
                                    } else {
                                        Triple(words[0], words[1], "")
                                    }
                                    if (step.duplicateCheck != null) {
                                        val unique = step.duplicateCheck.invoke(fio)
                                        if (!unique) {
                                            reply("Данное ФИО уже содержится в базе данных")
                                            false
                                        } else {
                                            memo = fio; true
                                        }
                                    } else {
                                        memo = fio; true
                                    }
                                }
                            }
                        }

                        is Step.PhotoStep -> {
                            if (step.skip && text == SKIP) {
                                memo = null; true
                            } else {
                                message.validatePhoto()
                                    .mapLeft { reply(it) }
                                    .map { memo = message.extract() }
                                    .isRight()
                            }
                        }

                        is Step.ConfirmStep -> {
                            if (text == "да") {
                                step.onSave.invoke(built.ctx)
                            } else {
                                step.onCancel.invoke(built.ctx)
                            }
                            true

                        }

                        is Step.MultiStep<CTX, *> -> {
                            if (memo == null) {
                                memo = mutableSetOf<Any>()
                                true
                            }

                            val multiStep = step as Step.MultiStep<CTX, Any>
                            val raw = text?.trim().orEmpty()
                            if (raw == COMPLETE) {
                                true
                            } else {
                                val parsed = multiStep.parse.invoke(raw)
                                if (parsed.isLeft()) {
                                    reply(parsed.leftOrNull() ?: "Неверный ввод")
                                    false
                                } else {
                                    val value = parsed.swap().leftOrNull()!!
                                    val data = (memo as MutableSet<Any>)
                                    if (data.contains(value)) {
                                        reply("Уже добавлено")
                                        false
                                    } else if (data.size >= multiStep.maxCount) {
                                        true
                                    } else {
                                        data.add(value)
                                        reply("Сохранено. Введите следующее или $COMPLETE")
                                        false
                                    }
                                }
                            }
                        }
                    }
                }

                onTriggered { _ ->
                    when (step) {
                        is Step.TextStep -> step.apply(built.ctx, memo as String)
                        is Step.IntStep -> step.apply(built.ctx, memo as Int)
                        is Step.DateStep -> step.apply(built.ctx, memo as LocalDate)
                        is Step.PhoneStep -> step.apply(built.ctx, memo as String)
                        is Step.EnumStep -> step.apply(built.ctx, memo as String)
                        is Step.FioStep -> step.apply(built.ctx, memo as FioValue)
                        is Step.PhotoStep -> step.apply(built.ctx, memo as String)
                        is Step.ConfirmStep -> {}
                        is Step.MultiStep<*, *> -> if (memo is Set<*>) {
                            val setAny = memo as Set<Any>
                            val multiStep = step as Step.MultiStep<CTX, Any>
                            multiStep.apply(built.ctx, setAny)
                        }
                    }
                }

                targetState = next
            }
        }

        onFinished { e ->
            built.onFinish.invoke((e.event as? TelegramEvent)?.message!!, built.ctx)
            Storage.remove(userId)

        }
    }
}


suspend fun <CTX : Any> BehaviourContext.startTelegramFsm(
    name: String,
    ctx: CTX,
    msg: CommonMessage<MessageContent>,
    build: FsmBuilder<CTX>.() -> Unit
): StateMachine {
    val sm = telegramFsm(name, ctx, build, msg.userId())
    sm.processEvent(Initial())
    return sm
}
