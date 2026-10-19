package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.extensions.api.telegramBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyConfig
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService
import me.centralhardware.znatoki.telegram.statistic.web.WebConfig
import me.centralhardware.znatoki.telegram.statistic.web.WebServer

/**
 * Entry point of the image: starts the web app (unless WEB_ENABLED=false) with its own send-only
 * bot client and the Firefly export retry, then runs the bot exactly as before.
 */
suspend fun main() {
    if (WebConfig.enabled) {
        runMigrations()
        WebServer.start(telegramBot(WebConfig.botToken))
    }
    if (FireflyConfig.enabled) CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        buildSchedule("0 */10 * * * *").asTzFlowWithDelays().collect {
            runCatching { db { FireflyService.retryPending() } }.onFailure { KSLog.warning("Firefly retry failed", it) }
        }
    }
    me.centralhardware.znatoki.telegram.statistic.bot.main()
}
