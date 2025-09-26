package me.centralhardware.znatoki.telegram.statistic.integration

import me.centralhardware.znatoki.telegram.statistic.TestDatabaseConfiguration

object IntegrationTestSetup {
    private var initialized = false

    @Synchronized
    fun initializeOnce() {
        if (!initialized) {
            // Настраиваем переменные через System Properties для тестов
            val testDataSource = TestDatabaseConfiguration.getDataSource()
            val jdbcUrl = testDataSource.connection.use { it.metaData.url }

            // Устанавливаем system properties для Config
            System.setProperty("DATASOURCE_URL", jdbcUrl)
            System.setProperty("DATASOURCE_USERNAME", "test")
            System.setProperty("DATASOURCE_PASSWORD", "test")

            TestDatabaseConfiguration.runMigrations()
            initialized = true
        }
    }
}