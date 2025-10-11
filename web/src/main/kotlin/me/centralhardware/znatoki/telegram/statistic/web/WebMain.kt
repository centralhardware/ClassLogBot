package me.centralhardware.znatoki.telegram.statistic.web

import me.centralhardware.znatoki.telegram.statistic.logging.LoggingConfig
import me.centralhardware.znatoki.telegram.statistic.runMigrations
import me.centralhardware.znatoki.telegram.statistic.service.StudentService

suspend fun main() {
    // Configure logging format
    LoggingConfig.configure()

    runMigrations()
    StudentService.init()
    me.centralhardware.znatoki.telegram.statistic.api.WebServer.start()
}
