package me.centralhardware.znatoki.telegram.statistic

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

@Testcontainers
abstract class TestDatabaseConfiguration {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")

        private var dataSource: DataSource? = null

        @JvmStatic
        fun getDataSource(): DataSource {
            if (!postgres.isRunning) {
                postgres.start()
            }

            // Return existing DataSource if already created
            dataSource?.let { return it }

            val config = HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                driverClassName = postgres.driverClassName
                maximumPoolSize = 20
                minimumIdle = 5
                connectionTimeout = 20000
                idleTimeout = 300000
                maxLifetime = 900000
                leakDetectionThreshold = 60000
            }

            return HikariDataSource(config).also {
                dataSource = it
            }
        }

        private var schemaInitialized = false

        @JvmStatic
        fun runMigrations() {
            if (schemaInitialized) return

            if (!postgres.isRunning) {
                postgres.start()
            }

            val currentDataSource = dataSource ?: getDataSource()

            // Используем Flyway для применения миграций
            val flyway = Flyway.configure()
                .dataSource(currentDataSource)
                .locations("classpath:migrations")
                .load()

            flyway.migrate()

            // Добавляем тестовые данные
            currentDataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    // Добавляем тестовые услуги
                    statement.execute("""
                        INSERT INTO services (id, name, allow_multiply_clients)
                        VALUES (1, 'Математика', false),
                               (2, 'Физика', true),
                               (3, 'Химия', false)
                        ON CONFLICT (id) DO NOTHING
                        """)

                    // Добавляем тестового пользователя
                    statement.execute("""
                        INSERT INTO telegram_users (id, name, services, permissions)
                        VALUES (12345, 'Test Tutor', '1:2', ARRAY['ADMIN'])
                        ON CONFLICT (id) DO NOTHING
                        """)
                }
            }
            schemaInitialized = true
        }
    }
}