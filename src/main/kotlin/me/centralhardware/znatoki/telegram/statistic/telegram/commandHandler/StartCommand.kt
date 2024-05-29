package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.bot

suspend fun startCommand(message: CommonMessage<TextContent>) = bot.sendMessage(message.chat, """
                         Бот предназначенный для анализа вашего бизнеса.
                         Автор: @centralhardware
                         """.trimIndent())