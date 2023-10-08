package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Service;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.PropertiesTypeHandler;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.UuidTypeHandler;
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
public interface ServiceMapper {

    @Select("""
            SELECT org_id
            FROM service
            WHERE id = #{id}
            LIMIT 1
            """)
    String __getOrgId(@Param("id") UUID orgId);

    default UUID getOrgId(UUID orgId){
        return UUID.fromString(__getOrgId(orgId));
    }

    @Insert("""
            INSERT INTO service (
                date_time,
                id,
                chat_id,
                service_id,
                amount,
                pupil_id,
                org_id,
                properties
            ) VALUES (
                #{service.dateTime},
                #{service.id},
                #{service.chatId},
                #{service.serviceId},
                #{service.amount},
                #{service.pupilId},
                #{service.organizationId},
                #{service.properties, typeHandler=me.centralhardware.znatoki.telegram.statistic.typeHandler.PropertiesTypeHandler}::JSONB
            )
            """)
    void insertTime(@Param("service") Service service);

    @Select("""
            SELECT date_time,
                   id,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   org_id,
                   properties
            FROM service
            WHERE id = #{id}
                AND is_deleted=false
            """)
    @Results({
            @Result(property = "dateTime", column = "date_time"),
            @Result(property = "id", column = "id", typeHandler = UuidTypeHandler.class),
            @Result(property = "chatId", column = "chat_id"),
            @Result(property = "serviceId", column = "service_id"),
            @Result(property = "pupilId", column = "pupil_id"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "organizationId", column = "org_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "properties", column = "properties", typeHandler = PropertiesTypeHandler.class)
    })
    List<Service> _getTimesById(@Param("id") UUID id);

    default List<Service> getTimes(UUID id){
        return convert(_getTimesById(id));
    }

    @Select("""
            SELECT date_time,
                   id,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   org_id,
                   properties
            FROM service
            WHERE chat_id = #{userId}
                AND date_time between to_timestamp(#{startDate}, 'DD-MM-YYYY HH24:MI:SS') and to_timestamp(#{endDate}, 'DD-MM-YYYY HH24:MI:SS')
                AND is_deleted=false
            """)
    @Results({
            @Result(property = "dateTime", column = "date_time"),
            @Result(property = "id", column = "id", typeHandler = UuidTypeHandler.class),
            @Result(property = "chatId", column = "chat_id"),
            @Result(property = "serviceId", column = "service_id"),
            @Result(property = "pupilId", column = "pupil_id"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "organizationId", column = "org_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "properties", column = "properties", typeHandler = PropertiesTypeHandler.class)
    })
    List<Service> _getTimesByChatId(@Param("userId") Long userId,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    default List<Service> _getTimesByChatId(Long userId,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate){
        return _getTimesByChatId(userId, dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate));
    }

    default List<Service> getTimes(Long userId,
                                   LocalDateTime startDate,
                                   LocalDateTime endDate) {
        return convert(_getTimesByChatId(userId, startDate, endDate));
    }

    default List<Service> convert(List<Service> rawServices){
        Multimap<UUID, Service> times = ArrayListMultimap.create();
        rawServices.forEach(it -> times.put(it.getId(), it));
        return times.asMap()
                .values()
                .stream()
                .map(timeCollection -> {
                    var service = timeCollection.stream().findFirst().get();
                    service.setServiceIds(timeCollection.stream().map(Service::getPupilId).collect(Collectors.toSet()));
                    return service;
                })
                .sorted(Comparator.comparing(Service::getDateTime))
                .toList();
    }



    default List<Service> getTodayTimes(Long chatId){
        return getTimes(chatId, LocalDateTime.now().with(LocalTime.MIN), LocalDateTime.now());
    }

    default List<Service> getCuurentMontTimes(Long chatId){
        return getTimes(chatId,
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now());
    }

    default List<Service> getPrevMonthTimes(Long chatId){
        return getTimes(chatId,
                LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59));
    }

    @Select("""
            SELECT DISTINCT chat_id
            FROM service
            WHERE is_deleted = false AND org_id = #{org_id}
            """)
    @ResultType(Long.class)
    List<Long> getIds(@Param("org_id") UUID orgId);

    @Update("""
            UPDATE service
            SET is_deleted = #{is_deleted}
            WHERE id = #{id}
            """)
    void setDeleted(@Param("id") UUID timeId, @Param("is_deleted") Boolean isDeleted);

    @Select("""
            SELECT DISTINCT service_id
            FROM service
            WHERE pupil_id = #{id} ANd is_deleted=false
            """)
    List<Long> getServicesForPupil(@Param("id") Integer id);

}
