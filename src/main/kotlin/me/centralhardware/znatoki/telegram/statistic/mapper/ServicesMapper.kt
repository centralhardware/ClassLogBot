package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session

object ServicesMapper {

    fun exists(name: String): Boolean =
        session.run(
            queryOf(
                    """
            SELECT exists(
                SELECT *
                FROM  services
                WHERE name = :name
            ) as exists
            """, mapOf("name" to name)
                )
                .map { it.boolean("exists") }
                .asSingle
        ) ?: false

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
