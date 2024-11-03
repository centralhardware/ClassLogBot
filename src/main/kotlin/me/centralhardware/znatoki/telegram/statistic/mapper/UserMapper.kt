package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.containsAny
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.entity.parseUser

object UserMapper {

    fun getAdminsId(): List<Long> =
        session.run(
            queryOf(
                    """
               SELECT id
               FROM telegram_users
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
                    mapOf("id" to id),
                )
                .map { it.parseUser() }
                .asSingle
        )

    fun hasReadRight(id: Long): Boolean {
        return (findById(id)?.permissions?.size ?: 0) > 0
    }

    fun hasPaymentPermission(id: Long): Boolean {
        return findById(id)?.permissions?.containsAny(Permissions.ADD_PAYMENT, Permissions.ADMIN)
            ?: false
    }

    fun hasTimePermission(id: Long): Boolean {
        return findById(id)?.permissions?.containsAny(Permissions.ADD_TIME, Permissions.ADMIN)
            ?: false
    }

    fun hasClientPermission(id: Long): Boolean {
        return findById(id)?.permissions?.containsAny(Permissions.ADD_CLIENT, Permissions.ADMIN)
            ?: false
    }

    fun hasAdminPermission(id: Long): Boolean {
        return findById(id)?.permissions?.contains(Permissions.ADMIN) ?: false
    }
}
