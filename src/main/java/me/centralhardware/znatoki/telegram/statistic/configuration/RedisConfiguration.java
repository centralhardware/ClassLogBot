package me.centralhardware.znatoki.telegram.statistic.configuration;

import me.centralhardware.znatoki.telegram.statistic.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfiguration {

    @Bean
    public JedisPool getJedis(){
        return new JedisPool(Config.getRedisHost(), Config.getRedisPort());
    }

}
