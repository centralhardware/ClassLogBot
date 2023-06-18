package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfiguration {

    @Bean
    public RateLimiter getRateLimiter(){
        return RateLimiter.create(Double.parseDouble(System.getenv("TELEGRAM_RATE_LIMIT")));
    }

}
