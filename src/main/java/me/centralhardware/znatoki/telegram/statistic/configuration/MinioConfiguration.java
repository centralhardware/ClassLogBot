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
                .endpoint(Config.getMinioUrl(), Config.getMinioPort(), false)
                .credentials(Config.getMinioAccessKey(),
                        Config.getMinioSecretKey())
                .build();
    }

}
