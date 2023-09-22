package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Mapper
public interface ServicesMapper {

    @Insert("""
            """)
    void insert(@Param("orgId") UUID orgId, @Param("key") String key, @Param("name") String name);

    default void insert(Set<String> services, UUID orgId, Function<String, String> transliterate){
        services.forEach(it -> insert(orgId, transliterate.apply(it).toUpperCase(), it));
    }

}
