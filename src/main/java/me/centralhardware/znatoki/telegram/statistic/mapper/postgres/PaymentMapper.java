package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Payment;
import org.apache.ibatis.annotations.*;

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

}
