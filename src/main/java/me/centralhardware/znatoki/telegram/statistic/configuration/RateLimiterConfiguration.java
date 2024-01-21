package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.google.common.util.concurrent.RateLimiter;
import me.centralhardware.znatoki.telegram.statistic.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("UnstableApiUsage")
@Configuration
public class RateLimiterConfiguration {

    @Bean
    public RateLimiter getRateLimiter(){
        return RateLimiter.create(Config.Telegram.getTelegramRateLimit());
    }

}
