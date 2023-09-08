package me.centralhardware.znatoki.telegram.statistic.mapper;

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

}
