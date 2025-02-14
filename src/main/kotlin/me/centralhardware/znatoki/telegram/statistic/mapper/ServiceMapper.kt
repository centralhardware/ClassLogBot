package me.centralhardware.znatoki.telegram.statistic.mapper

import java.time.LocalDateTime
import java.util.*
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.parseTime
import me.centralhardware.znatoki.telegram.statistic.extensions.endOfMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfDay
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth

object ServiceMapper {

    fun insert(service: Service) =
        session.execute(
            queryOf(
                """
            INSERT INTO service (
                date_time,
                update_time,
                id,
                chat_id,
                service_id,
                amount,
                pupil_id,
                properties
            ) VALUES (
                :dateTime,
                :updateTime,
                :id,
                :chatId,
                :serviceId,
                :amount,
                :clientId,
                :properties::JSONB
            )
            """,
                mapOf(
                    "dateTime" to service.dateTime,
                    "updateTime" to service.updateTime,
                    "id" to service.id,
                    "chatId" to service.chatId,
                    "serviceId" to service.serviceId,
                    "amount" to service.amount,
                    "clientId" to service.clientId,
                    "properties" to service.properties.toJson(),
                ),
            )
        )

    private fun findAllByUserId(
        userId: Long,
        serviceId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Service> =
        session.run(
            queryOf(
                    """
            SELECT id,
                   date_time,
                   update_time,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   properties,
                   force_group,
                   extra_half_hour,
                   is_deleted
            FROM service
            WHERE chat_id = :userId
                AND service_id = :serviceId
                AND date_time between :startDate and :endDate
                AND is_deleted=false
            """,
                    mapOf("userId" to userId,"serviceId" to serviceId, "startDate" to startDate, "endDate" to endDate),
                )
                .map { it.parseTime() }
                .asList
        )

    private fun findAllByUserId(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Service> =
        session.run(
            queryOf(
                """
            SELECT id,
                   date_time,
                   update_time,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   properties,
                   force_group,
                   extra_half_hour,
                   is_deleted
            FROM service
            WHERE chat_id = :userId
                AND date_time between :startDate and :endDate
                AND is_deleted=false
            """,
                mapOf("userId" to userId, "startDate" to startDate, "endDate" to endDate),
            )
                .map { it.parseTime() }
                .asList
        )

    fun findById(id: UUID): List<Service> =
        session.run(
            queryOf(
                    """
            SELECT id,
                   date_time,
                   update_time,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   properties,
                   force_group,
                   extra_half_hour,
                   is_deleted
            FROM service
            WHERE id = :id
            """,
                    mapOf("id" to id),
                )
                .map { it.parseTime() }
                .asList
        )

    fun getTodayTimes(chatId: Long): List<Service> =
        findAllByUserId(chatId, LocalDateTime.now().startOfDay(), LocalDateTime.now())

    fun getCurrentMontTimes(chatId: Long, serviceId: Long): List<Service> = findAllByUserId(
        chatId,
        serviceId,
        LocalDateTime.now().startOfMonth(),
        LocalDateTime.now().endOfMonth(),
    )

    fun getPrevMonthTimes(chatId: Long, serviceId: Long): List<Service> = findAllByUserId(
        chatId,
        serviceId,
        LocalDateTime.now().prevMonth().startOfMonth(),
        LocalDateTime.now().prevMonth().endOfMonth(),
    )

    fun getIds(): List<Long> =
        session.run(
            queryOf(
                    """
            SELECT DISTINCT chat_id
            FROM service
            WHERE is_deleted = false
            """
                )
                .map { row -> row.long("chat_id") }
                .asList
        )

    fun setDeleted(timeId: UUID, isDeleted: Boolean) =
        session.run(
            queryOf(
                    """
            UPDATE service
            SET is_deleted = :is_deleted,
                update_time = :update_time
            WHERE id = :id
            """,
                    mapOf(
                        "id" to timeId,
                        "is_deleted" to isDeleted,
                        "update_time" to LocalDateTime.now(),
                    ),
                )
                .asUpdate
        )

    fun getServicesForClient(id: Int): List<Long> =
        session.run(
            queryOf(
                    """
            SELECT DISTINCT service_id
            FROM service
            WHERE pupil_id = :id ANd is_deleted=false
            """,
                    mapOf("id" to id),
                )
                .map { row -> row.long("service_id") }
                .asList
        )

    fun setForceGroup(id: UUID, forceGroup: Boolean) =
        session.run(
            queryOf(
                    """
        UPDATE service 
        SET force_group = :force_group,
            update_time = :update_time
        WHERE id = :id
    """,
                    mapOf(
                        "id" to id,
                        "force_group" to forceGroup,
                        "update_time" to LocalDateTime.now(),
                    ),
                )
                .asUpdate
        )

    fun setExtraHalfHour(id: UUID, forceGroup: Boolean) =
        session.run(
            queryOf(
                """
        UPDATE service 
        SET extra_half_hour = :extra_half_hour,
            update_time = :update_time
        WHERE id = :id
    """,
                mapOf(
                    "id" to id,
                    "extra_half_hour" to forceGroup,
                    "update_time" to LocalDateTime.now(),
                ),
            )
                .asUpdate
        )

}
