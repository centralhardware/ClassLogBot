package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.toSubjectId
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle

object SubjectMapper {

    fun getIdByName(name: String): SubjectId =
        session.runSingle(
            queryOf(
                """
            SELECT id
            FROM services
            WHERE name = :name
            """,
                mapOf("name" to name),
            )
        ) {
            row -> row.long("id").toSubjectId()
        } ?: throw IllegalArgumentException("No subject with name $name found")

    fun getNameById(id: SubjectId): String =
        session.runSingle(
            queryOf(
                """
            SELECT name
            FROM  services
            WHERE id = :id
            """,
                mapOf("id" to id.id),
            )
        ) {
            row -> row.string("name")
        } ?: throw IllegalArgumentException("No subject with id $id found")

    fun isAllowGroup(id: SubjectId): Boolean = session.runSingle(
        queryOf(
            """
            SELECT allow_multiply_clients
            FROM services
            WHERE id = :id
            """,
            mapOf("id" to id.id),
        )
    ) {
        row -> row.boolean("allow_multiply_clients")
    } ?: throw IllegalArgumentException("No subject with id $id found")

}
