package me.centralhardware.znatoki.telegram.statistic.logging

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.*

/**
 * Configures java.util.logging for KSLog with a clean, readable format
 */
object LoggingConfig {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    
    fun configure() {
        val rootLogger = LogManager.getLogManager().getLogger("")
        
        // Remove default handlers
        rootLogger.handlers.forEach { rootLogger.removeHandler(it) }
        
        // Add console handler with custom formatter
        val consoleHandler = ConsoleHandler().apply {
            level = Level.ALL
            formatter = CompactFormatter()
        }
        
        rootLogger.addHandler(consoleHandler)
        rootLogger.level = Level.INFO
    }
    
    private class CompactFormatter : Formatter() {
        override fun format(record: LogRecord): String {
            val timestamp = ZonedDateTime.now().format(dateFormatter)
            val level = record.level.name
            val loggerName = record.loggerName?.let { name ->
                // Shorten logger name for readability
                name.split(".").lastOrNull() ?: name
            } ?: "Unknown"
            
            val message = formatMessage(record)
            
            val throwable = record.thrown?.let { throwable ->
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                "\n$sw"
            } ?: ""
            
            return "$timestamp [$level] $loggerName - $message$throwable\n"
        }
    }
}
