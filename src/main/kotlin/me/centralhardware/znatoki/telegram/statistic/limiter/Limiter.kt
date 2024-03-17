package me.centralhardware.znatoki.telegram.statistic.limiter

import com.google.common.util.concurrent.RateLimiter
import org.springframework.stereotype.Component

@Suppress("UnstableApiUsage")
@Component
class Limiter(private val rateLimiter: RateLimiter) {

    fun limit(block: () -> Unit) {
        rateLimiter.acquire()
        block.invoke()
    }
}