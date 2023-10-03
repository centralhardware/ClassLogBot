package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.Organization;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.CustomPropertiesTypeHandler;
import org.apache.ibatis.annotations.*;

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
                #{org.id},
                #{org.name},
                #{org.owner}
            )
            """)
    void insert(@Param("org") Organization organization);

    @Update("""
            UPDATE organization
            SET log_chat_id = #{log_chat_id}
            """)
    void updateLogChat(@Param("org_id") UUID orgId, @Param("log_chat_id") Long logChat);

    @Select("""
            SELECT *
            FROM organization
            WHERE owner = #{id}
            """)
    @Results({
            @Result(property = "logChatId", column = "log_chat_id"),
            @Result(property = "serviceCustomProperties", column = "service_custom_properties"),
            @Result(property = "clientCustomProperties", column = "client_custom_properties"),
            @Result(property = "paymentCustomProperties", column = "payment_custom_properties")
    })
    Organization getByOwner(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM organization
            WHERE id = #{id}
            """)
    @Results({
            @Result(property = "logChatId", column = "log_chat_id"),
            @Result(property = "serviceCustomProperties", column = "service_custom_properties", typeHandler = CustomPropertiesTypeHandler.class),
            @Result(property = "clientCustomProperties", column = "client_custom_properties", typeHandler = CustomPropertiesTypeHandler.class),
            @Result(property = "paymentCustomProperties", column = "payment_custom_properties", typeHandler = CustomPropertiesTypeHandler.class)
    })
    Organization getById(@Param("id") UUID id);



    @Select("""
            SELECT *
            FROM organization
            """)
    @Results({
            @Result(property = "logChatId", column = "log_chat_id")
    })
    List<Organization> getOwners();

    @Select("""
            SELECT exists(SELECT id
                          FROM  organization
                          WHERE owner = #{owner_id})
            """)
    Boolean exist(@Param("owner_id") Long OwnerId);

}
