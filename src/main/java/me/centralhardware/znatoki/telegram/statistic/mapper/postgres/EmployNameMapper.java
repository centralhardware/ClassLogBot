package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface EmployNameMapper {

    @Select("""
            SELECT fio
            FROM employ_name
            WHERE chat_id = #{chatId}
            """)
    String getFio(@Param("chatId") Long chatId);

    @Insert("""
            INSERT INTO employ_name(
                chat_id,
                fio
            ) VALUES (
                #{chat_id},
                #{fio}
            );commit
            """)
    void insert(@Param("chat_id") Long chatId, @Param("fio") String fio);

}
