package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.Organization;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.simpleframework.xml.core.Commit;

import java.util.List;

@Mapper
public interface OrganizationMapper {

    @Insert("""
            INSERT INTO organization(
                id,
                name,
                owner
            ) VALUES (
                #{org.id},
                #{org.name},
                #{org.owner}
            )
            """)
    @Commit
    void insert(@Param("org") Organization organization);


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

    @Select("""
            SELECT exists(SELECT id
                          FROM  organization
                          WHERE owner = #{owner_id})
            """)
    Boolean exist(@Param("owner_id") Long OwnerId);

}
