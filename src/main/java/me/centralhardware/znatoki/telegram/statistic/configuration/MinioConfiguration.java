package me.centralhardware.znatoki.telegram.statistic.configuration;

import io.minio.MinioClient;
import me.centralhardware.znatoki.telegram.statistic.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {

    @Bean
    public MinioClient getMinioClient(){
        return MinioClient.builder()
                .endpoint(Config.Minio.getMinioUrl(), Config.Minio.getMinioPort(), false)
                .credentials(Config.Minio.getMinioAccessKey(),
                        Config.Minio.getMinioSecretKey())
                .build();
    }

}
