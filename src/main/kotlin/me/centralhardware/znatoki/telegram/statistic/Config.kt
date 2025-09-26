package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.tgbotapi.types.toChatId

object Config {

    private fun getEnvOrThrow(name: String): String =
        System.getenv(name) ?: System.getProperty(name) ?: throw IllegalStateException("Environment variable '$name' is required but not set")

    object Minio {
        val url: String         by lazy { getEnvOrThrow("MINIO_URL") }
        val bucket: String      by lazy { getEnvOrThrow("MINIO_BUCKET") }
        val accessKey: String   by lazy { getEnvOrThrow("MINIO_ACCESS_KEY") }
        val secretKey: String   by lazy { getEnvOrThrow("MINIO_SECRET_KEY") }
        val basePath: String    by lazy { getEnvOrThrow("BASE_PATH") }
    }

    object Datasource {
        val url: String         by lazy { getEnvOrThrow("DATASOURCE_URL") }
        val username: String    by lazy { getEnvOrThrow("DATASOURCE_USERNAME") }
        val password: String    by lazy { getEnvOrThrow("DATASOURCE_PASSWORD") }
    }

    fun logChat() = try {
        getEnvOrThrow("LOG_CHAT").toLong().toChatId()
    } catch (e: NumberFormatException) {
        throw IllegalStateException("Environment variable 'LOG_CHAT' must be a valid number, got: '${System.getenv("LOG_CHAT")}'", e)
    }

}
