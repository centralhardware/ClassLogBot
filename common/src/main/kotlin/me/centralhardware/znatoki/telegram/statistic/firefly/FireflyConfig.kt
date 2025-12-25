package me.centralhardware.znatoki.telegram.statistic.firefly

object FireflyConfig {
    val fireflyBaseUrl: String = System.getenv("FIREFLY_BASE_URL")

    val fireflyToken: String = System.getenv("FIREFLY_TOKEN")
        ?: throw IllegalArgumentException("FIREFLY_TOKEN environment variable is not set")

    val enabled: Boolean = System.getenv("FIREFLY_ENABLED")?.toBoolean() ?: false
}
