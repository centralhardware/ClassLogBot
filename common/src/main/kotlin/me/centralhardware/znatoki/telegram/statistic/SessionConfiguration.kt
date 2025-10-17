package me.centralhardware.znatoki.telegram.statistic

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.output.MigrateResult
import javax.sql.DataSource

private fun createDataSource(): DataSource {
    val config = HikariConfig().apply {
        jdbcUrl = Config.Datasource.url
        username = Config.Datasource.username
        password = Config.Datasource.password

        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 30_000
        idleTimeout = 600_000
        maxLifetime = 1_800_000

        isAutoCommit = true
        transactionIsolation = "TRANSACTION_READ_COMMITTED"

        connectionTestQuery = "SELECT 1"
        validationTimeout = 5_000
    }

    return HikariDataSource(config)
}

val dataSource: DataSource by lazy {
    try {
        createDataSource()
    } catch (e: Exception) {
        throw RuntimeException("Failed to initialize database connection pool", e)
    }
}

fun runMigrations(): MigrateResult {
    return try {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:migrations")
            .cleanDisabled(true)
            .baselineOnMigrate(true)  // Automatically baseline for existing databases
            .baselineVersion("000")
            .baselineDescription("Initial schema from existing database")
            .callbacks(*emptyArray<Callback>())  // Clear default callback locations for fat JAR compatibility
            .load()

        KSLog.info("Starting database migrations...")
        val result = flyway.migrate()
        KSLog.info("Database migrations completed successfully. Applied ${result.migrationsExecuted} migrations.")
        result
    } catch (e: Exception) {
        throw RuntimeException("Failed to run database migrations", e)
    }
}
