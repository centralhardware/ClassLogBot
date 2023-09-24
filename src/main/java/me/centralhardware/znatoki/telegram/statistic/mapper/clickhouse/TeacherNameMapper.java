package me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface TeacherNameMapper {

    @Select("""
            SELECT fio
            FROM znatoki_statistic_teacher_name
            WHERE chat_id = #{chatId}
            """)
    String getFio(@Param("chatId") Long chatId);

    @Insert("""
            INSERT INTO znatoki_statistic_teacher_name(
                chat_id, 
                fio
            ) VALUES (
                #{chat_id},
                #{fio}
            )
            """)
    void insert(@Param("chat_id") Long chatId, @Param("fio") String fio);

}
