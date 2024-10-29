package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Services
import me.centralhardware.znatoki.telegram.statistic.entity.parseServices

object ServicesMapper {

    fun findAll(): List<Services> =
        session.run(
            queryOf(
                    """
            SELECT *
            FROM  services
            """
                )
                .map { it.parseServices() }
                .asList
        )

    fun getServiceId(services: String): Long? =
        session.run(
            queryOf(
                    """
            SELECT id
            FROM services
            WHERE name = :name
            """,
                    mapOf("name" to services),
                )
                .map { row -> row.long("id") }
                .asSingle
        )

    fun getNameById(id: Long): String? =
        session.run(
            queryOf(
                    """
            SELECT name
            FROM  services
            WHERE id = :id
            """,
                    mapOf("id" to id),
                )
                .map { row -> row.string("name") }
                .asSingle
        )

    fun isAllowMultiplyClients(id: Long): Boolean? =
        session.run(
            queryOf(
                    """
            SELECT allow_multiply_clients
            FROM services
            WHERE id = :id
            """,
                    mapOf("id" to id),
                )
                .map { row -> row.boolean("allow_multiply_clients") }
                .asSingle
        )
}
