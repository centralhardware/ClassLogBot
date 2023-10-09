package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Session;
import org.apache.ibatis.annotations.*;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface SessionMapper {

    @Select("""
            SELECT *
            FROM session
            WHERE uuid = CAST(#{id} as varchar)
            """)
    @Results({
            @Result(property = "uuid", column = "uuid"),
            @Result(property = "clientId", column = "client_id"),
            @Result(property = "createDate", column = "create_date"),
            @Result(property = "updateBy", column = "update_by")
    })
    Optional<Session> findByUUid(@Param("id") UUID id);

    @Insert("""
            INSERT INTO session (
                uuid,
                create_date,
                client_id,
                update_by
            ) VALUES (
                #{session.uuid},
                #{session.createDate},
                #{session.clientId},
                #{session.updateBy}
            )
            """)
    void save(@Param("session") Session session);

}
