package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.tgbotapi.extensions.api.telegramBot
import me.centralhardware.znatoki.telegram.statistic.web.WebConfig
import me.centralhardware.znatoki.telegram.statistic.web.WebServer

/**
 * Entry point of the web image: migrates the database, then serves the Mini App and its API. Talks to
 * Telegram only through a send-only client (notifications, reminders, log messages); updates are the
 * bot image's job.
 */
fun main() {
    runMigrations()
    WebServer.start(telegramBot(WebConfig.botToken))
}
