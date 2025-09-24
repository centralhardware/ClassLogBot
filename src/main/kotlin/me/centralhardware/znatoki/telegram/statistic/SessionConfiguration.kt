package me.centralhardware.znatoki.telegram.statistic

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import javax.sql.DataSource

private fun createDataSource(): DataSource {
    val config = HikariConfig().apply {
        jdbcUrl = Config.Datasource.url
        username = Config.Datasource.username
        password = Config.Datasource.password

        // Connection pool settings
        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 30_000
        idleTimeout = 600_000
        maxLifetime = 1_800_000

        // Performance settings
        isAutoCommit = true
        transactionIsolation = "TRANSACTION_READ_COMMITTED"

        // Health check
        connectionTestQuery = "SELECT 1"
        validationTimeout = 5_000
    }

    return HikariDataSource(config)
}

private val dataSource: DataSource by lazy {
    try {
        createDataSource()
    } catch (e: Exception) {
        throw RuntimeException("Failed to initialize database connection pool", e)
    }
}

val session get() = sessionOf(dataSource)
