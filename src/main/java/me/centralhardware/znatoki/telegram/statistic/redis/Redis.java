package me.centralhardware.znatoki.telegram.statistic.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Configuration;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class Redis {


    private final JedisPool pool;
    private final ObjectMapper mapper;
    private final Configuration configuration;

    public <V> void put(String key, V value){
        if (configuration.getIsDemo()) return;

        execute(jedis -> Try.of(() -> jedis.set(key, mapper.writeValueAsString(value))).get());
    }

    public <V> Try<V> get(String key, Class<V> clazz){
        if (configuration.getIsDemo()){
            return (Try<V>) Try.success(new ZnatokiUser(List.of(Subject.LITERATURE, Subject.BIOLOGY)));
        }
        
        return execute(jedis -> Try.of(() -> mapper.readValue(jedis.get(key), clazz)));
    }

    public <V> void sadd(String key, V value){
        executeVoid(jedis -> jedis.sadd(key, value.toString()));
    }

    public <T> Boolean sismember(String key, T value){
        return execute(jedis -> jedis.sismember(key, value.toString()));
    }

    public <T> void srem(String key, T value){
        executeVoid(jedis -> jedis.srem(key, value.toString()));
    }

    public Boolean exists(String key){
        return execute(jedis -> jedis.exists(key));
    }

    public void executeVoid(Consumer<Jedis> operation){
        execute(jedis -> {operation.accept(jedis);return Void.class;});
    }

    public <V> V execute(Function<Jedis, V> operation){
        try (Jedis jedis = pool.getResource()){
            return operation.apply(jedis);
        }
    }

}
