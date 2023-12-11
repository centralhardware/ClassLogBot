package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Payment;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

public interface PaymentMapper {

    @Insert("""
            INSERT INTO payment(
                date_time,
                chat_id,
                pupil_id,
                amount,
                time_id,
                org_id,
                properties
            ) VALUES (
                #{payment.dateTime},
                #{payment.chatId},
                #{payment.pupilId},
                #{payment.amount},
                #{payment.timeId},
                #{payment.organizationId},
                #{payment.properties, typeHandler=me.centralhardware.znatoki.telegram.statistic.typeHandler.PropertiesTypeHandler}::JSONB
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(@Param("payment") Payment payment);

    @Update("""
            UPDATE payment
            SET is_deleted = #{is_delete}
            WHERE time_id = #{time_id}
            """)
    void setDeleteByTimeId(@Param("time_id") UUID timeId, @Param("is_delete") Boolean isDelete);

    @Update("""
            UPDATE payment
            SET is_deleted = #{is_delete}
            WHERE id = #{id}
            """)
    void setDelete(@Param("id") Integer id, @Param("is_delete") Boolean isDelete);


    @Select("""
            SELECT org_id
            FROM payment
            WHERE id = #{id}::int
            """)
    String __getOrgById(@Param("id") Integer id);

    default UUID getOrgById(Integer id){
        return UUID.fromString(__getOrgById(id));
    }

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Select("""
            SELECT sum(amount)
            FROM payment
            WHERE chat_id = #{chat_id}
                AND pupil_id = #{pupil_id}
                AND date_time between to_timestamp(#{startDate}, 'DD-MM-YYYY HH24:MI:SS') and to_timestamp(#{endDate}, 'DD-MM-YYYY HH24:MI:SS')
            """)
    Integer __getPaymentsSumByPupil(@Param("chat_id") Long chatId,
                             @Param("pupil_id") Integer pupilId,
                             @Param("startDate") String startDate,
                             @Param("endDate") String endDate);

    default Integer getPaymentsSumByPupil(Long chatId,
                                   Integer pupilId,
                                   LocalDateTime date){
        var res = __getPaymentsSumByPupil(chatId,
                pupilId,
                dateTimeFormatter.format(date.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)),
                dateTimeFormatter.format(date.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withSecond(59)));

        return res == null?
                0:
                res;
    }

    @Select("""
            SELECT sum(amount)
            FROM payment
            WHERE chat_id = #{chat_id}
                AND date_time between to_timestamp(#{startDate}, 'DD-MM-YYYY HH24:MI:SS') and to_timestamp(#{endDate}, 'DD-MM-YYYY HH24:MI:SS')
            """)
    Integer __getPaymentsSum(@Param("chat_id") Long chatId,
                             @Param("startDate") String startDate,
                             @Param("endDate") String endDate);

    default Integer getPaymentsSum(Long chatId,
                                   LocalDateTime date){
        var res = __getPaymentsSum(chatId,
                dateTimeFormatter.format(date.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)),
                dateTimeFormatter.format(date.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withSecond(59)));

        return res == null?
                0:
                res;
    }

}
