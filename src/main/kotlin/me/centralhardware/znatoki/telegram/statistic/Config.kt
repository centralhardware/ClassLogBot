package me.centralhardware.znatoki.telegram.statistic

object Config {

    object Telegram {
        val token: String = System.getenv("BOT_TOKEN")
        val rateLimit: Double = System.getenv("TELEGRAM_RATE_LIMIT")?.toDouble()!!
        val startTelegraph: String = System.getenv("START_TELEGRAPH")
    }

    object Minio {
        val url: String = System.getenv("MINIO_URL")
        val bucket: String = System.getenv("MINIO_BUCKET")
        val port: Int = System.getenv("MINIO_PORT")?.toInt()!!
        val accessKey: String = System.getenv("MINIO_ACCESS_KEY")
        val secretKey: String = System.getenv("MINIO_SECRET_KEY")
        val basePath: String = System.getenv("BASE_PATH")
    }

}