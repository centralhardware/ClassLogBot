package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.entity.parseUser

object UserMapper {

    fun getAll(): List<TelegramUser> =
        session.run(
            queryOf(
                """
                   SELECT *
                   FROM telegram_users
                """
            )
                .map { it -> it.parseUser() }
                .asList
        )

    fun getAdminsId(): List<Long> =
        session.run(
            queryOf(
                    """
               SELECT id
               FROM telegram_users
               WHERE 'ADMIN' = ANY(permissions)           
            """
                )
                .map { it.long("id") }
                .asList
        )

    fun findById(id: Long): TelegramUser? =
        session.run(
            queryOf(
                    """
            SELECT *
            FROM telegram_users
            WHERE id = :id
            """,
                    mapOf("id" to id),
                )
                .map { it.parseUser() }
                .asSingle
        )
}
