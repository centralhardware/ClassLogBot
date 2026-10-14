package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.tgbotapi.extensions.api.telegramBot
import me.centralhardware.znatoki.telegram.statistic.web.WebConfig
import me.centralhardware.znatoki.telegram.statistic.web.WebServer

/**
 * Entry point of the image: starts the web app (unless WEB_ENABLED=false) with its own send-only
 * bot client, then runs the bot exactly as before.
 */
suspend fun main() {
    if (WebConfig.enabled) {
        runMigrations()
        WebServer.start(telegramBot(WebConfig.botToken))
    }
    me.centralhardware.znatoki.telegram.statistic.bot.main()
}
