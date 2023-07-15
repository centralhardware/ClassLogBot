package me.centralhardware.znatoki.telegram.statistic.mapper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.UuidTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TimeMapper {

    @Insert("""
            INSERT INTO default.znatoki_statistic_time (
                date_time,
                id, 
                chat_id, 
                subject, 
                fio, 
                amount, 
                photoId
            ) VALUES (
                #{time.dateTime},
                #{time.id},
                #{time.chatId},
                #{time.subject},
                #{time.fio},
                #{time.amount},
                #{time.photoId}
            )
            """)
    void insertTime(@Param("time") Time time);

    @Select("""
            SELECT date_time,
                   id,
                   chat_id, 
                   subject, 
                   fio, 
                   amount, 
                   photoId
            FROM znatoki_statistic_time
            WHERE chat_id = #{userId} 
                AND date_time between toStartOfDay(today()) and date_time
                AND is_deleted=false
            """)
    @Results({
            @Result(property = "dateTime", column = "date_time"),
            @Result(property = "id", column = "id", typeHandler = UuidTypeHandler.class),
            @Result(property = "chatId", column = "chat_id"),
            @Result(property = "subject", column = "subject"),
            @Result(property = "fio", column = "fio"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "photoId", column = "photoId")
    })
    List<Time> _getTodayTimes(@Param("userId") Long userId);

    default List<Time> getTodayTimes(Long userId) {
        Multimap<UUID, Time> times = ArrayListMultimap.create();
        _getTodayTimes(userId).forEach(it -> times.put(it.getId(), it));
        return times.asMap()
                .values()
                .stream()
                .map(timeCollection -> {
                    var time = timeCollection.stream().findFirst().get();
                    time.setFios(timeCollection.stream().map(Time::getFio).toList());
                    return time;
                })
                .toList();
    }

    @Select("""
            SELECT DISTINCT chat_id
            FROM default.znatoki_statistic_time
            """)
    @ResultType(Long.class)
    List<Long> getIds();

}
