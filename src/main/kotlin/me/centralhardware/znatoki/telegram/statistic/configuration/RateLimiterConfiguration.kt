package me.centralhardware.znatoki.telegram.statistic.configuration

import com.google.common.util.concurrent.RateLimiter
import me.centralhardware.znatoki.telegram.statistic.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Suppress("UnstableApiUsage")
@Configuration
class RateLimiterConfiguration {

    @Bean
    fun getRateLimiter(): RateLimiter {
        return RateLimiter.create(Config.Telegram.rateLimit)
    }

}