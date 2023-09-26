package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.Invitation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvitationMapper {

    @Insert("""
            INSERT INTO invintation (
                org_id,
                services,
                confirm_code
            ) VALUES (
                #{invitation.orgId},
                #{invitation.services},
                #{invitation.confirmCode}
            )
            """)
    void insert(@Param("invitation") Invitation invitation);

}
