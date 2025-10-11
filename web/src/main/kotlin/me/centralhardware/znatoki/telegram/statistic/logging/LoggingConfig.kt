package me.centralhardware.znatoki.telegram.statistic.logging

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.KSLoggerDefaultPlatformLoggerLambda
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.setDefaultKSLog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Configures KSLog with a clean, readable format
 */
object LoggingConfig {

    private val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")

    private fun getDateTime(): String = LocalDateTime.now().format(formatter)

    fun configure() {
        KSLoggerDefaultPlatformLoggerLambda =
            fun(_, _, message, throwable) {
                println("${getDateTime()} $message")
                if (throwable != null) {
                    println(throwable.stackTraceToString())
                }
            }

        setDefaultKSLog(KSLog("WebApp", minLoggingLevel = LogLevel.INFO))
    }
}
