package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.eav.types.Telephone

private fun Property.applyTypeFormat(): Property {
    return if (this.type is Telephone) {
        this.value.formatTelephone()?.let { this.withValue(it) } ?: this
    } else {
        this
    }
}

fun List<Property>.print(): String {
    return this.filterNot { it.type is Photo }
        .map { it.applyTypeFormat() }
        .joinToString("\n") { property -> "${property.name}=${property.value.makeBold()}" }
}

suspend fun PropertiesBuilder.process(
    message: CommonMessage<MessageContent>,
    onFinish: (List<Property>) -> Unit,
): Boolean {
    var isFinished = false
    validate(message)
        .mapLeft { error -> bot.sendTextMessage(message.chat, error) }
        .map {
            setProperty(message)

            next()?.let { next ->
                if (next.second.isNotEmpty()) {
                    runBlocking {
                        bot.send(
                            message.chat,
                            text = next.first,
                            replyMarkup =
                                replyKeyboard { next.second.forEach { row { simpleButton(it) } } },
                        )
                    }
                } else {
                    bot.sendTextMessage(
                        message.chat,
                        next.first,
                        replyMarkup = ReplyKeyboardRemove(),
                    )
                }
            }
                ?: run {
                    onFinish(properties)
                    isFinished = true
                }
        }
    return isFinished
}

fun List<Property>.find(name: String) = first { it.name == name }