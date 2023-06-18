package me.centralhardware.znatoki.telegram.statistic.configuration;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {

    @Bean
    public MinioClient getMinioClient(){
        return MinioClient.builder()
                .endpoint(System.getenv("MINIO_URL"), Integer.parseInt(System.getenv("MINIO_PORT")), false)
                .credentials(System.getenv("MINIO_ACCESS_KEY"),
                        System.getenv("MINIO_SECRET_KEY"))
                .build();
    }

}
