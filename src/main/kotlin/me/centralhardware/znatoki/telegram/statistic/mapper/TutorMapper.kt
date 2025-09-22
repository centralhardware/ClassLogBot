package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parseUser
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle

object TutorMapper {

    fun getAdminsId(): List<Long> =
        session.runList(
            queryOf(
                    """
               SELECT id
               FROM telegram_users
               WHERE 'ADMIN' = ANY(permissions)           
            """
                )
        ) { it.long("id") }

    fun findByIdOrNull(id: TutorId): Tutor? =
        session.runSingle(
            queryOf(
                    """
            SELECT *
            FROM telegram_users
            WHERE id = :id
            """,
                    mapOf("id" to id),
                )
        ) { it.parseUser() }
}
