package me.centralhardware.znatoki.telegram.statistic

object Config {

    object Minio {
        val url: String = System.getenv("MINIO_URL")
        val bucket: String = System.getenv("MINIO_BUCKET")
        val accessKey: String = System.getenv("MINIO_ACCESS_KEY")
        val secretKey: String = System.getenv("MINIO_SECRET_KEY")
        val basePath: String = System.getenv("BASE_PATH")
    }

    object Datasource {
        val url: String = System.getenv("DATASOURCE_URL")
        val username: String = System.getenv("DATASOURCE_USERNAME")
        val password: String = System.getenv("DATASOURCE_PASSWORD")
    }
}
