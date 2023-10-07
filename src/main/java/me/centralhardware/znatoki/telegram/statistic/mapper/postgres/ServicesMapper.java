package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.Services;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ServicesMapper {

    @Insert("""
            INSERT INTO services (
                key,
                name,
                organization_id
            ) VALUES (
                #{services.key},
                #{services.name},
                #{services.orgId}
            )
            """)
    void insert(@Param("services") Services services);

    @Select("""
            SELECT *
            FROM  services
            WHERE organization_id = #{orgId}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "name", column = "name"),
            @Result(property = "orgId", column = "organization_id")
    })
    List<Services> getServicesByOrganization(@Param("orgId") UUID orgId);

    @Select("""
            SELECT id
            FROM services
            WHERE name = #{name} AND organization_id = #{org_id}
            """)
    Long getServiceId(@Param("org_id") UUID orgId, @Param("name") String services);

    @Select("""
            SELECT name
            FROM  services
            WHERE id = #{id}
            """)
    String getNameById(@Param("id") Long id);

    @Select("""
            SELECT name
            FROM  services
            WHERE id = #{id}
            """)
    String getKeyById(@Param("id") Long id);

}
