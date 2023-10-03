package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.Payment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
    void insert(@Param("payment") Payment payment);

    @Update("""
            UPDATE payment
            SET is_deleted = #{is_delete}
            WHERE time_id = #{time_id}
            """)
    void setDeleteByTimeId(@Param("time_id") UUID timeId, @Param("is_delete") Boolean isDelete);

}
