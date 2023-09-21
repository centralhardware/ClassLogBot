package me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
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
                pupil_id,
                organization_id
            ) VALUES (
                #{time.dateTime},
                #{time.id},
                #{time.chatId},
                #{time.subject},
                #{time.fio},
                #{time.amount},
                #{time.photoId},
                #{time.pupilId},
                #{time.organizationId}
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
            WHERE id = #{id}
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
    List<Time> _getTimesById(@Param("id") UUID id);

    default List<Time> getTimes(UUID id){
        return convert(_getTimesById(id));
    }

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
    List<Time> _getTimesByChatId(@Param("userId") Long userId,
                             @Param("startDate") String startDate,
                             @Param("endDate") String endDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    default List<Time> _getTimesByChatId(Long userId,
                                     LocalDateTime startDate,
                                     LocalDateTime endDate){
        return _getTimesByChatId(userId, dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate));
    }

    default List<Time> getTimes(Long userId,
                                LocalDateTime startDate,
                                LocalDateTime endDate) {
        return convert(_getTimesByChatId(userId, startDate, endDate));
    }

    default List<Time> convert(List<Time> rawTimes){
        Multimap<UUID, Time> times = ArrayListMultimap.create();
        rawTimes.forEach(it -> times.put(it.getId(), it));
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
            WHERE is_deleted = false AND organization_id = #{org_id}
            """)
    @ResultType(Long.class)
    List<Long> getIds(@Param("org_id") UUID orgId);

    @Update("""
            ALTER TABLE znatoki_statistic_time UPDATE is_deleted = #{is_deleted} WHERE id = #{id}
            """)
    void setDeleted(@Param("id") UUID timeId, @Param("is_deleted") Boolean isDeleted);

    @Select("""
            SELECT DISTINCT subject
            FROM znatoki_statistic_time
            WHERE toInt32(pupil_id) = toInt32(#{id})
            """)
    List<Subject> getSubjectsForPupil(@Param("id") Integer id);

}
