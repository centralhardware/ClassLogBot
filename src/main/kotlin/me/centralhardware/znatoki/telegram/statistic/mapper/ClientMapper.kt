package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.parseClient
import me.centralhardware.znatoki.telegram.statistic.toJson

object ClientMapper {

    fun existsByFio(fio: String): Boolean =
        session.run(
            queryOf(
                    """
            SELECT EXISTS(
            SELECT *
            FROM client
            WHERE lower(trim(concat(id, ' ', name, ' ', second_name, ' ', last_name))) = lower(:fio)
            ORDER BY create_date DESC
            ) as e
        """,
                    mapOf("fio" to fio),
                )
                .map { row -> row.boolean("e") }
                .asSingle
        ) ?: false

    fun save(client: Client): Int =
        session.run(
            queryOf(
                    """
               INSERT INTO client (
                        create_date,
                        last_name,
                        modify_date,
                        name,
                        second_name,
                        created_by,
                        deleted,
                        properties
               ) VALUES (
                    :create_date,
                    :last_name,
                    :modify_date,
                    :name,
                    :second_name,
                    :created_by,
                    :deleted,
                    :properties::JSONB
               ) RETURNING id
            """,
                    mapOf(
                        "create_date" to client.createDate,
                        "last_name" to client.lastName,
                        "modify_date" to client.modifyDate,
                        "name" to client.name,
                        "second_name" to client.secondName,
                        "created_by" to client.createdBy,
                        "deleted" to client.deleted,
                        "properties" to client.properties.toJson(),
                    ),
                )
                .map { it.int("id") }
                .asSingle
        )!!

    fun delete(id: Int) =
        session.update(
            queryOf(
                """
               UPDATE client 
               SET deleted = true
               WHERE id = :id
            """,
                mapOf("id" to id),
            )
        )

    fun findById(id: Int): Client? =
        session.run(
            queryOf(
                    """
                SELECT *
                FROM client
                WHERE id = :id
            """,
                    mapOf("id" to id),
                )
                .map { it.parseClient() }
                .asSingle
        )

    fun findAll(): List<Client> =
        session.run(
            queryOf(
                    """
                SELECT *
                FROM client
                WHERE deleted = false
            """
                )
                .map { it.parseClient() }
                .asList
        )

    fun findAllByFio(name: String, secondName: String, lastName: String): List<Client> =
        session.run(
            queryOf(
                    """
                SELECT *
                FROM client
                WHERE name = :name AND second_name = :secondName AND last_name = :lastName
            """,
                    mapOf("name" to name, "second_name" to secondName, "last_name" to lastName),
                )
                .map { it.parseClient() }
                .asList
        )

    fun getFioById(id: Int): String =
        session.run(
            queryOf(
                    """
            SELECT concat(name, ' ', last_name, ' ', second_name) as fio
            from client
            WHERE id = :id
        """,
                    mapOf("id" to id),
                )
                .map { row -> row.string("fio") }
                .asSingle
        ) ?: ""
}
