package me.centralhardware.znatoki.telegram.statistic

object Config {

    object Telegram {
        val token: String = System.getenv("BOT_TOKEN")
    }

    object Minio {
        val url: String = System.getenv("MINIO_URL")
        val bucket: String = System.getenv("MINIO_BUCKET")
        val port: Int = System.getenv("MINIO_PORT")?.toInt()!!
        val accessKey: String = System.getenv("MINIO_ACCESS_KEY")
        val secretKey: String = System.getenv("MINIO_SECRET_KEY")
        val basePath: String = System.getenv("BASE_PATH")
        val proxyUrl: String = System.getenv("PROXY_URL")
    }

    object Datasource {
        val url: String = System.getenv("DATASOURCE_URL")
        val username: String = System.getenv("DATASOURCE_USERNAME")
        val password: String = System.getenv("DATASOURCE_PASSWORD")
    }
}
