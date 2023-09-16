package me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.UuidTypeHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper
public interface TimeMapper {

    @Insert("""
            INSERT INTO znatoki_statistic_time (
                date_time,
                id,
                chat_id,
                subject,
                fio,
                amount,
                photoId,
                pupil_id
            ) VALUES (
                #{time.dateTime},
                #{time.id},
                #{time.chatId},
                #{time.subject},
                #{time.fio},
                #{time.amount},
                #{time.photoId},
                #{time.pupilId}
            )
            """)
    void insertTime(@Param("time") Time time);

    @Select("""
            SELECT date_time,
                   id,
                   chat_id,
                   subject,
                   fio,
                   pupil_id,
                   amount,
                   photoId
            FROM znatoki_statistic_time
            WHERE chat_id = #{userId}
                AND date_time between toDateTime(#{startDate}) and toDateTime(#{endDate})
                AND is_deleted=false
            """)
    @Results({
            @Result(property = "dateTime", column = "date_time"),
            @Result(property = "id", column = "id", typeHandler = UuidTypeHandler.class),
            @Result(property = "chatId", column = "chat_id"),
            @Result(property = "subject", column = "subject"),
            @Result(property = "fio", column = "fio"),
            @Result(property = "pupilId", column = "pupil_id"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "photoId", column = "photoId")
    })
    List<Time> _getTimes(@Param("userId") Long userId,
                         @Param("startDate") String startDate,
                         @Param("endDate") String endDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    default List<Time> _getTimes(Long userId,
                         LocalDateTime startDate,
                         LocalDateTime endDate){
        return _getTimes(userId, dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate));
    }

    default List<Time> getTimes(Long userId,
                                LocalDateTime startDate,
                                LocalDateTime endDate) {
        Multimap<UUID, Time> times = ArrayListMultimap.create();
        _getTimes(userId, startDate, endDate).forEach(it -> times.put(it.getId(), it));
        return times.asMap()
                .values()
                .stream()
                .map(timeCollection -> {
                    var time = timeCollection.stream().findFirst().get();
                    time.setFios(timeCollection.stream().map(it -> Pair.of(it.getFio(), it.getPupilId())).collect(Collectors.toSet()));
                    return time;
                })
                .sorted(Comparator.comparing(Time::getDateTime))
                .toList();
    }

    default List<Time> getTodayTimes(Long chatId){
        return getTimes(chatId, LocalDateTime.now().with(LocalTime.MIN), LocalDateTime.now());
    }

    default List<Time> getCuurentMontTimes(Long chatId){
        return getTimes(chatId,
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now());
    }

    default List<Time> getPrevMonthTimes(Long chatId){
        return getTimes(chatId,
                LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59));
    }

    @Select("""
            SELECT DISTINCT chat_id
            FROM znatoki_statistic_time
            WHERE is_deleted = false
            """)
    @ResultType(Long.class)
    List<Long> getIds();

}
