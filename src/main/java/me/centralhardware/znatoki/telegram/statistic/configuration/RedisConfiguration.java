package me.centralhardware.znatoki.telegram.statistic.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfiguration {

    @Bean
    public JedisPool getJedis(){
        return new JedisPool(System.getenv("REDIS_HOST"),
                Integer.parseInt(System.getenv("REDIS_PORT")));
    }

}
