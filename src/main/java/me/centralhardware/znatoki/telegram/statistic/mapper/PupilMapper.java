package me.centralhardware.znatoki.telegram.statistic.mapper;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Pupil;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Mapper
public interface PupilMapper {

    @Select("""
            SELECT class_number,
                    date_of_birth,
                    last_name,
                    name,
                    second_name 
            FROM pupil 
            WHERE deleted = false
            """)
    @ConstructorArgs({
            @Arg(column = "class_number", javaType = Integer.class),
            @Arg(column = "date_of_birth", javaType = LocalDateTime.class),
            @Arg(column = "last_name", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "second_name", javaType = String.class)
    })
    List<Pupil> getPupils();

    @Select("""
            SELECT 1
            FROM pupil 
            WHERE trim(lowerUTF8(concat(name, ' ', second_name, ' ',  last_name))) = #{fio} 
                AND deleted = FALSE
            """)
    Integer _exist(@Param("fio") String fio);

    default Boolean exist(String fio){
        return Objects.equals(1, _exist(fio));
    }

}
