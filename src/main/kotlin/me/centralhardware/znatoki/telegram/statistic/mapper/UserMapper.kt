package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.entity.parseUser

object UserMapper {

    fun getAdminsId(): List<Long> =
        session.run(
            queryOf(
                    """
               SELECT id
               WHERE role = 'ADMIN'
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
                    mapOf("id" to id)
                )
                .map { it.parseUser() }
                .asSingle
        )

    fun hasWriteRight(id: Long): Boolean {
        return findById(id)?.role?.let { it == Role.READ_WRITE || it == Role.ADMIN } ?: false
    }

    fun hasReadRight(id: Long): Boolean {
        return findById(id)?.role?.let {
            it == Role.READ || it == Role.READ_WRITE || it == Role.ADMIN
        }
            ?: false
    }

    fun hasAdminRight(id: Long): Boolean {
        return findById(id)?.role?.let { it == Role.ADMIN } ?: false
    }
}
