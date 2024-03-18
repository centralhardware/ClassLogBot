package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Session
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Organization
import me.centralhardware.znatoki.telegram.statistic.entity.parseOrganization
import me.centralhardware.znatoki.telegram.statistic.parseStringList
import org.springframework.stereotype.Component
import java.util.*

@Component
class OrganizationMapper(private val session: Session) {

    fun updateLogChat(orgId: UUID, logChat: Long) {
        session.run(
            queryOf(
                """
            UPDATE organization
            SET log_chat_id = :log_chat_id
            WHERE id = :org_id
            """,
                mapOf(
                    "log_chat_id" to logChat,
                    "org_id" to orgId
                )
            ).asUpdate
        )
    }

    fun getByOwner(id: Long): Organization? = session.run(
        queryOf(
            """
            SELECT *
            FROM organization
            WHERE owner = :id
            """, mapOf("id" to id)
        ).map{ it.parseOrganization() }.asSingle
    )

    fun getById(id: UUID): Organization? = session.run(
        queryOf(
            """
            SELECT *
            FROM organization
            WHERE id = :id
            """, mapOf("id" to id)
        ).map { it.parseOrganization() }.asSingle
    )

    fun getOwners(): List<Organization> = session.run(
        queryOf(
            """
            SELECT *
            FROM organization
            """
        ).map{ it.parseOrganization() }.asList
    )

    fun exist(ownerId: Long): Boolean = session.run(
        queryOf(
            """
            SELECT exists(SELECT id
                          FROM  organization
                          WHERE owner = :owner_id) as e
            """, mapOf("owner_id" to ownerId)
        ).map { row -> row.boolean("e") }.asSingle
    )?: false

    fun getInlineFields(id: UUID): List<String> = session.run(
        queryOf(
            """
            SELECT include_in_inline
            FROM organization
            WHERE id = :id
            LIMIT 1
        """, mapOf("id" to id)
        ).map { row -> row.string("include_in_inline").parseStringList() }.asSingle
    )?: listOf()

    fun getReportFields(id: UUID): List<String> = session.run(
        queryOf(
            """
            SELECT include_in_report
            FROM organization
            WHERE id = :id
            LIMIT 1
        """, mapOf("id" to id)
        ).map { row -> row.string("include_in_report").parseStringList() }.asSingle
    )?: listOf()
}