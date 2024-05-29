package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Services
import me.centralhardware.znatoki.telegram.statistic.entity.parseServices
import java.util.*

object ServicesMapper {


    fun getServicesByOrganization(orgId: UUID): List<Services> = session.run(
        queryOf(
            """
            SELECT *
            FROM  services
            WHERE organization_id = :orgId
            """, mapOf("orgId" to orgId)
        ).map{ it.parseServices() }.asList
    )

    fun getServiceId(orgId: UUID, services: String): Long? = session.run(
        queryOf(
            """
            SELECT id
            FROM services
            WHERE name = :name AND organization_id = :org_id
            """, mapOf(
                "org_id" to orgId,
                "name" to services
            )
        ).map { row -> row.long("id") }.asSingle
    )

    fun getNameById(id: Long): String? = session.run(
        queryOf(
            """
            SELECT name
            FROM  services
            WHERE id = :id
            """, mapOf("id" to id)
        ).map { row -> row.string("name") }.asSingle
    )

    fun isAllowMultiplyClients(id: Long): Boolean? = session.run(
        queryOf(
            """
            SELECT allow_multiply_clients
            FROM services
            WHERE id = :id
            """, mapOf("id" to id)
        ).map { row -> row.boolean("allow_multiply_clients") }.asSingle
    )
}