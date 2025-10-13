package me.centralhardware.znatoki.telegram.statistic.web

import me.centralhardware.znatoki.telegram.statistic.api.WebServer
import me.centralhardware.znatoki.telegram.statistic.logging.LoggingConfig
import me.centralhardware.znatoki.telegram.statistic.runMigrations
import me.centralhardware.znatoki.telegram.statistic.service.StudentService

fun main() {
    LoggingConfig.configure()

    runMigrations()
    StudentService.init()
    WebServer.start()
}
