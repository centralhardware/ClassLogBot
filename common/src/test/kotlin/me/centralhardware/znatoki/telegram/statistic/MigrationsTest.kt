package me.centralhardware.znatoki.telegram.statistic

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer

/** Applies every migration to an empty Postgres. Skipped where Docker isn't available. */
class MigrationsTest {

    @Test
    fun `all migrations apply to an empty database`() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not available")

        PostgreSQLContainer("postgres:17-alpine").use { pg ->
            pg.start()
            val result = Flyway.configure()
                .dataSource(pg.jdbcUrl, pg.username, pg.password)
                .locations("classpath:migrations")
                .cleanDisabled(true)
                .load()
                .migrate()
            assertTrue(result.success)
        }
    }
}
