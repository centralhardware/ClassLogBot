package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.webAppButton
import dev.inmo.tgbotapi.types.webapps.WebAppInfo
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.Config

fun BehaviourContext.webReportCommand() = onCommand("web_report") { message ->
    val webAppUrl = Config.getString("WEB_APP_URL")

    sendMessage(
        message.chat,
        "Открыть веб-отчёты:",
        replyMarkup = inlineKeyboard {
            row {
                webAppButton(
                    "📊 Отчёты и статистика",
                    WebAppInfo("$webAppUrl/report.html")
                )
            }
        }
    )
}
