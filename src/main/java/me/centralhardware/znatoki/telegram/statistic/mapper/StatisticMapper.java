package me.centralhardware.znatoki.telegram.statistic.mapper;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.LogEntry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface StatisticMapper {

    @Insert("""
            INSERT INTO default.znatoki_statistic (
                date_time,
                chat_id,
                username,
                first_name,
                last_name,
                lang,
                is_premium,
                action,
                text
            ) VALUES (
                #{entry.dateTime},
                #{entry.chatId},
                #{entry.username},
                #{entry.firstName},
                #{entry.lastName},
                #{entry.lang},
                #{entry.isPremium},
                #{entry.action},
                #{entry.text}
            )
            """)
    void insertStatistic(@Param("entry") LogEntry entry);

}
