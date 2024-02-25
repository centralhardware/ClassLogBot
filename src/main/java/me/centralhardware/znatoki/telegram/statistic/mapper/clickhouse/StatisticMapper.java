package me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse;

import me.centralhardware.znatoki.telegram.statistic.entity.clickhouse.LogEntry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatisticMapper {

    @Insert("""
                   INSERT INTO default.bot_log (
                            date_time,
                            bot_name,
                            user_id,
                            usernames,
                            first_name,
                            last_name,
                            is_premium,
                            is_inline,
                            lang,
                            text
                       ) VALUES (
                            now(),
                            'znatokiStatistic',
                            #{entry.chatId},
                            array(#{entry.username}),
                            #{entry.firstName},
                            #{entry.lastName},
                            #{entry.isPremium},
                            False,
                            #{entry.lang},
                            #{entry.text}
                       )
            """)
    void insertStatistic(@Param("entry") LogEntry entry);

}
