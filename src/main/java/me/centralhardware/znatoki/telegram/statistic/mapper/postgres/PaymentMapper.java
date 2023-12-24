package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Payment;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
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
                properties,
                services
            ) VALUES (
                #{payment.dateTime},
                #{payment.chatId},
                #{payment.clientId},
                #{payment.amount},
                #{payment.timeId},
                #{payment.organizationId},
                #{payment.properties, typeHandler=me.centralhardware.znatoki.telegram.statistic.typeHandler.PropertiesTypeHandler}::JSONB,
                #{payment.serviceId}
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

    @Select("""
            SELECT sum(amount)
            FROM payment
            WHERE chat_id = #{chat_id}
                AND services = #{service_id}
                AND pupil_id = #{client_id}
                AND date_time between #{startDate} and #{endDate}
                AND jsonb_array_length(properties) > 0
                AND is_deleted = false
            """)
    Integer __getPaymentsSumByClient(@Param("chat_id") Long chatId,
                                     @Param("service_id") Long serviceId,
                                     @Param("client_id") Integer clientId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    default Integer getPaymentsSumByClient(Long chatId,
                                           Long serviceId,
                                           Integer clientId,
                                           LocalDateTime date){
        var res = __getPaymentsSumByClient(chatId,
                serviceId,
                clientId,
                date.with(TemporalAdjusters.firstDayOfMonth()),
                date.with(TemporalAdjusters.lastDayOfMonth()));

        return res == null?
                0:
                res;
    }

    @Select("""
            SELECT sum(amount)
            FROM payment
            WHERE chat_id = #{chat_id}
                AND services = #{service_id}
                AND date_time between #{startDate} and #{endDate}
                AND jsonb_array_length(properties) > 0
                AND is_deleted = false
            """)
    Integer __getPaymentsSum(@Param("chat_id") Long chatId,
                             @Param("service_id") Long serviceId,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    default Integer getPaymentsSum(Long chatId,
                                   Long serviceId,
                                   LocalDateTime date){
        var res = __getPaymentsSum(chatId,
                serviceId,
                date.with(TemporalAdjusters.firstDayOfMonth()),
                date.with(TemporalAdjusters.lastDayOfMonth()));

        return res == null?
                0:
                res;
    }

    @Select("""
            SELECT sum(amount)
            FROM payment
            WHERE pupil_id = #{client_id}
                AND is_deleted = false
            """)
    Integer getCredit(@Param("client_id") Integer clientId);

    @Select("""
            SELECT EXISTS(
                SELECT amount
                FROM payment 
                WHERE amount > 0 AND pupil_id = #{client_id}
                    AND is_deleted = false
            )
            """)
    Boolean paymentExists(@Param("client_id") Integer clientId);

}
