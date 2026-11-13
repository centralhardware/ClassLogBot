package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyConfig
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService

/**
 * Entry point of the bot image: migrates the database, starts the Firefly export retry, then runs the
 * bot. The web app is a separate image (see the web module).
 */
suspend fun main() {
    runMigrations()
    if (FireflyConfig.enabled) CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        buildSchedule("0 */10 * * * *").asTzFlowWithDelays().collect {
            runCatching { db { FireflyService.retryPending() } }.onFailure { KSLog.warning("Firefly retry failed", it) }
        }
    }
    me.centralhardware.znatoki.telegram.statistic.bot.main()
}
