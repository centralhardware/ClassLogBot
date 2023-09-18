package me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Payment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

public interface PaymentMapper {

    @Insert("""
            INSERT INTO znatoki_payment(
                date_time,
                chat_id,
                pupil_id,
                amount,
                photoId,
                time_id
            ) VALUES (
                #{payment.dateTime},
                #{payment.chatId},
                #{payment.pupilId},
                #{payment.amount},
                #{payment.photoId},
                #{payment.timeId}
            )
            """)
    void insert(@Param("payment")Payment payment);

    @Update("""
            ALTER TABLE znatoki_payment UPDATE is_deleted = #{is_delete} WHERE time_id = #{time_id}
            """)
    void setDeleteByTimeId(@Param("time_id") UUID timeId, @Param("is_delete") Boolean isDelete);

}
