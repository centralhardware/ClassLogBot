package me.centralhardware.znatoki.telegram.statistic.mapper;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

}
