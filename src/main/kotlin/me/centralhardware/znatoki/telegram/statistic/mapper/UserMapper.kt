package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.entity.parseUser
import me.centralhardware.znatoki.telegram.statistic.extensions.containsAny

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

    fun hasPaymentPermission(id: Long): Boolean =
        findById(id)?.permissions?.containsAny(Permissions.ADD_PAYMENT, Permissions.ADMIN) == true

    fun hasTimePermission(id: Long): Boolean =
        findById(id)?.permissions?.containsAny(Permissions.ADD_TIME, Permissions.ADMIN) == true

    fun hasClientPermission(id: Long): Boolean =
        findById(id)?.permissions?.containsAny(Permissions.ADD_CLIENT, Permissions.ADMIN) == true

    fun hasAdminPermission(id: Long): Boolean =
        findById(id)?.permissions?.contains(Permissions.ADMIN) == true

    fun hasForceGroup(id: Long): Boolean =
        findById(id)?.permissions?.contains(Permissions.FORCE_GROUP) == true
}
