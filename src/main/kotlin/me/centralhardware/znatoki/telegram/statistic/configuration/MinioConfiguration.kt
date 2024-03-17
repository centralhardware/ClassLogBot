package me.centralhardware.znatoki.telegram.statistic.configuration

import io.minio.MinioClient
import me.centralhardware.znatoki.telegram.statistic.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfiguration {

    @Bean
    fun getMinioClient(): MinioClient = MinioClient.builder()
            .endpoint(Config.Minio.url, Config.Minio.port, false)
            .credentials(Config.Minio.accessKey,
                    Config.Minio.secretKey)
            .build()

}