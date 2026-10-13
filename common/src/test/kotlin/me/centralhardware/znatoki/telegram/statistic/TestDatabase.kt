package me.centralhardware.znatoki.telegram.statistic

import kotliquery.queryOf
import kotliquery.sessionOf
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer

/**
 * One Postgres per test JVM, migrated with the real Flyway setup. The app's [dataSource] reads
 * its settings through [Config], which falls back to system properties, so they are set here
 * before anything touches it.
 */
object TestDatabase {

    val dockerAvailable: Boolean by lazy {
        runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)
    }

    private val started by lazy {
        val pg = PostgreSQLContainer("postgres:17-alpine")
        pg.start()
        System.setProperty("DATASOURCE_URL", pg.jdbcUrl)
        System.setProperty("DATASOURCE_USERNAME", pg.username)
        System.setProperty("DATASOURCE_PASSWORD", pg.password)
        runMigrations()
        true
    }

    fun start() = check(started)

    fun exec(sql: String, params: Map<String, Any?> = emptyMap()) {
        sessionOf(dataSource).use { it.execute(queryOf(sql, params)) }
    }
}
