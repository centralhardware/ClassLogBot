package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Session
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parseTutor
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update

object TutorMapper {

    context(_: Session)
    fun getAll(): List<Tutor> =
        runList(
            queryOf(
                """
                SELECT *
                FROM tutors
                """
            )
        ) { it -> it.parseTutor() }

    context(_: Session)
    fun getAdminsId(): List<TutorId> =
        runList(
            queryOf(
                """
                SELECT id
                FROM tutors
                WHERE 'ADMIN' = ANY(permissions)
                """
            )
        ) { TutorId(it.long("id")) }

    context(_: Session)
    fun findByIdOrNull(id: TutorId): Tutor? =
        runSingle(
            queryOf(
                """
                SELECT *
                FROM tutors
                WHERE id = :id
                """,
                    mapOf("id" to id.id),
                )
        ) { it.parseTutor() }

    context(_: Session)
    fun search(query: String): List<Tutor> =
        runList(
            queryOf(
                """
                SELECT *
                FROM tutors
                WHERE LOWER(name) LIKE LOWER(:query)
                LIMIT 10
                """,
                mapOf("query" to "%$query%")
            )
        ) { it.parseTutor() }

    context(_: Session)
    fun updateTutor(id: Long, permissions: List<String>, subjectIds: List<Long>, name: String? = null) {
        val subjectsString = subjectIds.joinToString(":")
        
        if (name != null) {
            update(
                queryOf(
                    """
                    UPDATE tutors
                    SET permissions = :permissions::text[],
                        subjects = :subjects,
                        name = :name
                    WHERE id = :id
                    """,
                    mapOf(
                        "id" to id,
                        "permissions" to permissions.toTypedArray(),
                        "subjects" to subjectsString,
                        "name" to name
                    )
                )
            )
        } else {
            update(
                queryOf(
                    """
                    UPDATE tutors
                    SET permissions = :permissions::text[],
                        subjects = :subjects
                    WHERE id = :id
                    """,
                    mapOf(
                        "id" to id,
                        "permissions" to permissions.toTypedArray(),
                        "subjects" to subjectsString
                    )
                )
            )
        }
    }
}
