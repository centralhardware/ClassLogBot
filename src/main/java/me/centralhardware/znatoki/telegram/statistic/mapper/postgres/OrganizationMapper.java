package me.centralhardware.znatoki.telegram.statistic.mapper.postgres;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Organization;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.CustomPropertiesTypeHandler;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.ListStringTypeHandler;
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
            @Result(property = "paymentCustomProperties", column = "payment_custom_properties"),
            @Result(property = "grafanaUsername", column = "grafana_username"),
            @Result(property = "grafanaPassword", column = "grafana_password"),
            @Result(property = "grafanaUrl", column = "grafana_url"),
            @Result(property = "clientName", column = "client_name")
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
            @Result(property = "paymentCustomProperties", column = "payment_custom_properties", typeHandler = CustomPropertiesTypeHandler.class),
            @Result(property = "grafanaUsername", column = "grafana_username"),
            @Result(property = "grafanaPassword", column = "grafana_password"),
            @Result(property = "grafanaUrl", column = "grafana_url"),
            @Result(property = "clientName", column = "client_name")
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

    @Select("""
            SELECT include_in_inline
            FROM organization
            WHERE id = #{id}
            LIMIT 1
            """)
    @Result(column = "include_in_inline", typeHandler = ListStringTypeHandler.class)
    List<String> getInlineFields(@Param("id") UUID id);

    @Select("""
            SELECT include_in_report
            FROM organization
            WHERE id = #{id}
            LIMIT 1
            """)
    @Result(column = "include_in_report", typeHandler = ListStringTypeHandler.class)
    List<String> getReportFields(@Param("id") UUID id);

}
