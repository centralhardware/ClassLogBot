package me.centralhardware.znatoki.telegram.statistic.telegram

import arrow.core.Either
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.d
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoAsynchronouslyWithUpdatesFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitContact
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDeepLinks
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitPoll
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineQueryInCurrentChatButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.extensions.extract
import me.centralhardware.znatoki.telegram.statistic.extensions.textOrNull
import java.time.LocalDate

const val SKIP = "/skip"
const val COMPLETE = "/complete"
const val CANCEL = "/cancel"

class ConversationCancelledException(message: String = "Conversation cancelled by user") : Exception(message)

private fun String.getOrCanceled() = if (this == CANCEL) throw ConversationCancelledException() else this
private fun Flow<CommonMessage<TextContent>>.filterChat(chatId: IdChatIdentifier) = filter { it.chat.id == chatId }

suspend fun BehaviourContext.startConversation(
    userId: Long,
    conversationType: ConversationState.ConversationType,
    block: suspend BehaviourContext.() -> Unit
) {
    val job = createSubContextAndDoAsynchronouslyWithUpdatesFilter(
        updatesUpstreamFlow = allUpdatesFlow
    ) {
        try {
            block()
        } catch (e: ConversationCancelledException) {
            throw e
        } finally {
            ConversationState.endConversation(userId)
        }
    }

    if (ConversationState.startConversation(userId, conversationType, job) == null) {
        job.cancel()
    }
}

fun switchToInlineKeyboard(searchType: String) = inlineKeyboard {
    row { inlineQueryInCurrentChatButton("Поиск", "$searchType ") }
}

val switchToInlineKeyboard = inlineKeyboard {
    row { inlineQueryInCurrentChatButton("inline", "") }
}

val yesNoKeyboard = replyKeyboard {
    row { simpleButton("да") }
    row { simpleButton("нет") }
}

fun createReplyKeyboard(options: List<String>) = replyKeyboard {
    options.forEach { option ->
        row { simpleButton(option) }
    }
}

/**
 * Wait for text message with validation
 */
suspend fun BehaviourContext.waitValidatedText(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    validator: (String) -> Either<String, Unit> = { Either.Right(Unit) },
    allowSkip: Boolean = false,
    useInline: Boolean = false,
    inlineSearchType: InlineSearchType = InlineSearchType.STUDENT,
    stepName: String? = null
): String? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        val replyMarkup = when {
            useInline -> {
                val prefix = when (inlineSearchType) {
                    InlineSearchType.TUTOR -> "t:"
                    InlineSearchType.STUDENT -> "s:"
                }
                switchToInlineKeyboard(prefix)
            }

            else -> ReplyKeyboardRemove()
        }

        sendTextMessage(chatId, prompt, replyMarkup = replyMarkup)

        val text = waitTextMessage()
            .filterChat(chatId)
            .first().content.text.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        validator(text).fold(
            { error -> sendTextMessage(chatId, error) },
            { return text }
        )
    }
}

/**
 * Wait for integer with validation
 */
suspend fun BehaviourContext.waitValidatedInt(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    validator: (Int?) -> Either<String, Unit> = { Either.Right(Unit) },
    allowSkip: Boolean = false,
    stepName: String? = null
): Int? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        sendTextMessage(chatId, prompt, replyMarkup = ReplyKeyboardRemove())

        val message = waitTextMessage()
            .filterChat(chatId)
            .first()

        val text = message.content.text.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        message.validateInt().fold(
            { error -> sendTextMessage(chatId, error) },
            {
                validator(text.toIntOrNull()).fold(
                    { error -> sendTextMessage(chatId, error) },
                    { return text.toInt() }
                )
            }
        )
    }
}

/**
 * Wait for date with validation
 */
suspend fun BehaviourContext.waitValidatedDate(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    allowSkip: Boolean = false,
    stepName: String? = null
): LocalDate? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        sendTextMessage(chatId, prompt, replyMarkup = ReplyKeyboardRemove())

        val message = waitTextMessage()
            .filterChat(chatId)
            .first()

        val text = message.content.text.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        message.validateDate().fold(
            { error -> sendTextMessage(chatId, error) },
            { return it }
        )
    }
}

/**
 * Wait for phone number with validation
 */
suspend fun BehaviourContext.waitValidatedPhone(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    allowSkip: Boolean = false,
    stepName: String? = null
): String? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        sendTextMessage(chatId, prompt, replyMarkup = ReplyKeyboardRemove())

        val message = waitTextMessage()
            .filterChat(chatId)
            .first()

        val text = message.content.text.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        message.validateTelephone().fold(
            { error -> sendTextMessage(chatId, error) },
            { return it }
        )
    }
}

/**
 * Wait for enum selection
 */
suspend fun BehaviourContext.waitEnum(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    options: List<String>,
    allowSkip: Boolean = false,
    stepName: String? = null
): String? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        send(chatId, text = prompt, replyMarkup = createReplyKeyboard(options))

        val message = waitTextMessage()
            .filterChat(chatId)
            .first()

        val text = message.content.text.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        message.validateEnum(options).fold(
            { error -> sendTextMessage(chatId, error) },
            { return it }
        )
    }
}

/**
 * Wait for FIO (Full name) with validation
 */
suspend fun BehaviourContext.waitValidatedFio(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    allowSkip: Boolean = false,
    duplicateCheck: (suspend (Triple<String, String, String>) -> Boolean)? = null,
    stepName: String? = null
): Triple<String, String, String>? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        sendTextMessage(chatId, prompt, replyMarkup = ReplyKeyboardRemove())

        val message = waitTextMessage()
            .filterChat(chatId)
            .first()

        val text = message.content.text.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        val raw = text.trim()
        val words = raw.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (words.size !in 2..3) {
            sendTextMessage(chatId, "ФИО требуется ввести в формате: фамилия имя [отчество]")
            continue
        }

        val fio = if (words.size == 3) {
            Triple(words[0], words[1], words[2])
        } else {
            Triple(words[0], words[1], "")
        }

        if (duplicateCheck != null) {
            val unique = duplicateCheck(fio)
            if (!unique) {
                sendTextMessage(chatId, "Данное ФИО уже содержится в базе данных")
                continue
            }
        }

        return fio
    }
}

/**
 * Wait for photo with validation
 */
suspend fun BehaviourContext.waitValidatedPhoto(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    allowSkip: Boolean = false,
    stepName: String? = null
): String? {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    while (true) {
        sendTextMessage(chatId, prompt, replyMarkup = ReplyKeyboardRemove())

        val message = waitContentMessage()
            .filter { it.chat.id == chatId }
            .first()

        val text = message.textOrNull()?.getOrCanceled()

        if (allowSkip && text == SKIP) {
            return null
        }

        message.validatePhoto().fold(
            { error -> sendTextMessage(chatId, error) },
            { return message.extract() }
        )
    }
}

/**
 * Wait for confirmation (yes/no)
 */
suspend fun BehaviourContext.waitConfirmation(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    stepName: String? = null
): Boolean {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    sendTextMessage(chatId, prompt, replyMarkup = yesNoKeyboard)

    return waitTextMessage()
        .filter { it.chat.id == chatId }
        .first().content.text.getOrCanceled() == "да"
}

/**
 * Wait for multiple values (used for collecting multiple items)
 */
suspend fun <T> BehaviourContext.waitMultiple(
    chatId: IdChatIdentifier,
    userId: Long,
    prompt: String,
    maxCount: Int = Int.MAX_VALUE,
    useInline: Boolean = false,
    parse: (String) -> Either<String, T>,
    stepName: String? = null
): Set<T> {
    stepName?.let { KSLog.d("User $userId -> step: $it") }
    val results = mutableSetOf<T>()

    while (results.size < maxCount) {
        val replyMarkup = if (useInline) switchToInlineKeyboard else ReplyKeyboardRemove()
        sendTextMessage(chatId, prompt, replyMarkup = replyMarkup)

        val text = waitTextMessage()
            .filterChat(chatId)
            .first().content.text.getOrCanceled()

        if (text == COMPLETE) {
            break
        }

        parse(text).fold(
            { error -> sendTextMessage(chatId, error) },
            { value ->
                if (results.contains(value)) {
                    sendTextMessage(chatId, "Уже добавлено")
                } else {
                    results.add(value)
                    sendTextMessage(chatId, "Сохранено. Введите следующее или $COMPLETE")
                }
            }
        )
    }

    return results
}
