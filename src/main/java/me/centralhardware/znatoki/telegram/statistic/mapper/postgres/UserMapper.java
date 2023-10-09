package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.ListLongTypeHandler;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.RoleTypeHandler;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT *
            FROM telegram_users
            WHERE id = #{id}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "role", column = "role", typeHandler = RoleTypeHandler.class),
            @Result(property = "organizationId", column = "org_id"),
            @Result(property = "services", column = "services", typeHandler = ListLongTypeHandler.class)
    })
    TelegramUser getById(@Param("id") Long id);

    @Insert("""
            INSERT INTO telegram_users (
                id,
                role,
                org_id,
                services
            ) VALUES (
                #{user.id},
                #{user.role, typeHandler=me.centralhardware.znatoki.telegram.statistic.typeHandler.RoleTypeHandler},
                #{user.organizationId},
                #{user.services, typeHandler=me.centralhardware.znatoki.telegram.statistic.typeHandler.ListLongTypeHandler}
            ) 
            """)
    void insert(@Param("user") TelegramUser user);



}
