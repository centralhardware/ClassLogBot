package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.entity.parseUser

object UserMapper {

    fun getById(id: Long): TelegramUser? = session.run(
        queryOf(
            """
            SELECT *
            FROM telegram_users
            WHERE id = :id
            """, mapOf("id" to id)
        ).map { it.parseUser() }.asSingle
    )

    fun hasWriteRight(id: Long): Boolean {
        return getById(id)?.role?.let {
            it == Role.READ_WRITE ||
                    it == Role.ADMIN
        } ?: false
    }

    fun hasReadRight(id: Long): Boolean {
        return getById(id)?.role?.let {
            it == Role.READ ||
                    it == Role.READ_WRITE ||
                    it == Role.ADMIN
        } ?: false
    }

    fun isAdmin(id: Long): Boolean {
        return getById(id)?.role?.let {
            it == Role.ADMIN
        } ?: false
    }

}