package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.toSubjectId
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.runList

object SubjectMapper {

    fun getIdByName(name: String): SubjectId =
        runSingle(
            queryOf(
                """
                SELECT id
                FROM subjects
                WHERE name = :name
                """,
                mapOf("name" to name),
            )
        ) {
            row -> row.long("id").toSubjectId()
        } ?: throw IllegalArgumentException("No subject with name $name found")

    fun getNameById(id: SubjectId): String =
        runSingle(
            queryOf(
                """
                SELECT name
                FROM subjects
                WHERE id = :id
                """,
                mapOf("id" to id.id),
            )
        ) {
            row -> row.string("name")
        } ?: throw IllegalArgumentException("No subject with id $id found")

    fun isAllowGroup(id: SubjectId): Boolean = runSingle(
        queryOf(
            """
            SELECT allow_multiply_clients
            FROM subjects
            WHERE id = :id
            """,
            mapOf("id" to id.id),
        )
    ) {
        row -> row.boolean("allow_multiply_clients")
    } ?: throw IllegalArgumentException("No subject with id $id found")

    data class SubjectDto(
        val subjectId: Long,
        val subjectName: String
    )

    fun getAllSubjects(): List<SubjectDto> =
        runList(
            queryOf(
                """
                SELECT id, name
                FROM subjects
                ORDER BY name
                """
            )
        ) { row ->
            SubjectDto(
                subjectId = row.long("id"),
                subjectName = row.string("name")
            )
        }

}
