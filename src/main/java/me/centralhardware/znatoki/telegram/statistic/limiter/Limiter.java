package me.centralhardware.znatoki.telegram.statistic.limiter;

import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@SuppressWarnings("UnstableApiUsage")
@Component
@Slf4j
@RequiredArgsConstructor
public class Limiter {

    private final RateLimiter rateLimiter;

    public void limit(Runnable runnable){
        rateLimiter.acquire();
        runnable.run();
    }


}
