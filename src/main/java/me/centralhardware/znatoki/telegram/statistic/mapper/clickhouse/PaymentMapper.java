package me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Payment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

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

}
