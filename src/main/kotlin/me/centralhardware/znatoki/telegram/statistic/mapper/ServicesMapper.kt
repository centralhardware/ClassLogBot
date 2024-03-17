package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Services
import org.springframework.stereotype.Component
import java.util.*

@Component
class ServicesMapper(private val session: Session) {

    fun insert(services: Services) = session.execute(
        queryOf(
            """
            INSERT INTO services (
                key,
                name,
                organization_id
            ) VALUES (
                :key,
                :name,
                :orgId
            )
            """, mapOf(
                "key" to services.key,
                "name" to services.name,
                "orgId" to services.orgId
            )
        )
    )

    val servicesMapper: (Row) -> Services = { row ->
        Services(
            row.long("id"),
            row.string("key"),
            row.string("name"),
            row.uuid("organization_id"),
            row.boolean("allow_multiply_clients")
        )
    }

    fun getServicesByOrganization(orgId: UUID): List<Services> = session.run(
        queryOf(
            """
            SELECT *
            FROM  services
            WHERE organization_id = :orgId
            """, mapOf("orgId" to orgId)
        ).map(servicesMapper).asList
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


    fun getKeyById(id: Long): String? = session.run(
        queryOf(
            """
            SELECT key
            FROM  services
            WHERE id = :id
            """, mapOf("id" to id)
        ).map { row -> row.string("key") }.asSingle
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