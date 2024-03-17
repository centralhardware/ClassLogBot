package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.parseLongList
import me.centralhardware.znatoki.telegram.statistic.toRole
import org.springframework.stereotype.Component

@Component
class UserMapper(private val session: Session) {

    val userMapper: (Row) -> TelegramUser = {
        row -> TelegramUser(
            row.long("id"),
            row.string("role").toRole(),
            row.uuid("org_id"),
            row.string("services").parseLongList(),
            row.string("name")
        )
    }

   fun getById(id: Long): TelegramUser? = session.run(
       queryOf("""
            SELECT *
            FROM telegram_users
            WHERE id = :id
            """, mapOf("id" to id)
       ).map(userMapper).asSingle
   )

   fun insert(user: TelegramUser) = session.execute(
       queryOf("""
           INSERT INTO telegram_users (
                id,
                role,
                org_id,
                services,
                name
            ) VALUES (
                :id,
                :role},
                :organizationId,
                :services,
                :name
            )
       """, mapOf("id" to user.id,
           "role" to user.role.name,
           "org_id" to user.organizationId,
           "services" to user.services.joinToString(":"))
       )
   )

}