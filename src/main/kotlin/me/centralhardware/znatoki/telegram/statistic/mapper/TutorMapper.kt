package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parseTutor
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle

object TutorMapper {

    fun getAll(): List<Tutor> =
        runList(
            queryOf(
                """
                SELECT *
                FROM tutors
                """
            )
        ) { it -> it.parseTutor() }

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
}
