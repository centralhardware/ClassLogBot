package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.Organization;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    @Insert("""
            INSERT INTO organization(
                id,
                name,
                owner
            ) VALUES (
                #{id},
                #{name},
                #{owner}
            )
                
            """)
    void insert(@Param("id") UUID id, @Param("name") String name, @Param("owner") Long owner);


    @Select("""
            SELECT *
            FROM organization
            WHERE owner = #{id}
            """)
    Organization getByOwner(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM organization
            """)
    List<Organization> getOwners();

}
