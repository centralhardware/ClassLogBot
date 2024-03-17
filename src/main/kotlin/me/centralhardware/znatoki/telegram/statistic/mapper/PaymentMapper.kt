package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Session
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.endOfMonth
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.startOfMonth
import me.centralhardware.znatoki.telegram.statistic.toJson
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class PaymentMapper(private val session: Session) {

    fun insert(payment: Payment): Int = session.updateAndReturnGeneratedKey(
        queryOf("""
            INSERT INTO payment(
                date_time,
                chat_id,
                pupil_id,
                amount,
                time_id,
                org_id,
                properties,
                services
            ) VALUES (
                :dateTime,
                :chatId,
                :clientId,
                :amount,
                :timeId,
                :organizationId,
                :properties::JSONB,
                :serviceId
            ) RETURNING id
            """, mapOf("dateTime" to payment.dateTime,
            "chatId" to payment.chatId,
            "clientId" to payment.clientId,
            "amount" to payment.amount,
            "timeId" to payment.timeId,
            "organizationId" to payment.organizationId,
            "properties" to payment.properties.toJson(),
            "serviceId" to payment.serviceId)
        )
    )?.toInt()!!

    fun setDeleteByTimeId(timeId: UUID, isDelete: Boolean) = session.run(
        queryOf("""
            UPDATE payment
            SET is_deleted = :is_delete
            WHERE time_id = :time_id
            """, mapOf("is_delete" to isDelete,
                "time_id" to timeId)
        ).asUpdate
    )

    fun setDelete(id: Int, isDelete: Boolean) = session.run(
        queryOf("""
            UPDATE payment
            SET is_deleted = :is_delete
            WHERE id = :id
            """, mapOf("id" to id,
                "is_delete" to isDelete)
        ).asUpdate
    )

    fun getOrgById(id: Int): UUID? = session.run(
        queryOf("""
            SELECT org_id
            FROM payment
            WHERE id = :id
            """, mapOf("id" to id)
        ).map { row -> row.uuid("org_id") }.asSingle
    )

    fun getPaymentsSumByClient(chatId: Long,
                                 serviceId: Long,
                                 clientId: Int,
                                 date: LocalDateTime
    ): Long = session.run(
        queryOf(
            """
            SELECT sum(amount) as sum
            FROM payment
            WHERE chat_id = :chat_id
                AND services = :service_id
                AND pupil_id = :client_id
                AND date_time between :startDate and :endDate
                AND jsonb_array_length(properties) > 0
                AND is_deleted = false
            """, mapOf(
                "chat_id" to chatId,
                "service_id" to serviceId,
                "client_id" to clientId,
                "startDate" to date.startOfMonth(),
                "endDate" to date.endOfMonth()
            )
        ).map { row -> row.longOrNull("sum") }.asSingle
    )?: 0

    fun getPaymentsSum(chatId: Long,
                         serviceId: Long,
                       date: LocalDateTime
    ): Long = session.run(
        queryOf(
            """
            SELECT sum(amount) as sum
            FROM payment
            WHERE chat_id = :chat_id
                AND services = :service_id
                AND date_time between :startDate and :endDate
                AND jsonb_array_length(properties) > 0
                AND is_deleted = false
            """, mapOf(
                "chat_id" to chatId,
                "service_id" to serviceId,
                "startDate" to date.startOfMonth(),
                "endDate" to date.endOfMonth()
            )
        ).map { row -> row.longOrNull("sum") }.asSingle
    )?: 0

    fun getCredit(clientId: Int): Int = session.run(
        queryOf("""
            SELECT sum(amount) as sum
            FROM payment
            WHERE pupil_id = :client_id
                AND is_deleted = false
            """, mapOf("client_id" to clientId)
        ).map { row -> row.int("sum") }.asSingle
    )?: 0

    fun paymentExists(clientId: Int): Boolean = session.run(
        queryOf("""
            SELECT EXISTS(
                SELECT amount
                FROM payment 
                WHERE amount > 0 AND pupil_id = :client_id
                    AND is_deleted = false
            )
            """, mapOf("client_id" to clientId)
        ).map { row -> row.boolean("amount") }.asSingle
    )?: false
}