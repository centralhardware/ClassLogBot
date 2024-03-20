package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.eav.types.Telephone
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import org.telegram.telegrambots.meta.api.objects.Update

private fun Property.applyTypeFormat(): Property {
    return if (this.type is Telephone) {
        this.value.formatTelephone()?.let { this.withValue(it) }?: this
    } else {
        this
    }
}

fun List<Property>.print(): String {
    return this
        .filterNot { it.type is Photo }
        .map { it.applyTypeFormat() }
        .joinToString("\n") { property -> "${property.name}=${property.value.makeBold()}" }
}

fun PropertiesBuilder.process(
    update: Update,
    onFinish: (List<Property>) -> Unit
): Boolean {
    val chatId = update.userId()

    var isFinished = false
    validate(update)
        .mapLeft { error ->
            sender().sendText(error, chatId)
        }
        .map {
            setProperty(update)

            next()?.let { next ->
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
                onFinish(properties)
                isFinished = true
            }
        }
    return isFinished
}