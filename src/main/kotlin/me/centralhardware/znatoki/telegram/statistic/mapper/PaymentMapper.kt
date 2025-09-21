package me.centralhardware.znatoki.telegram.statistic.mapper

import java.time.LocalDateTime
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.parsePayment
import me.centralhardware.znatoki.telegram.statistic.extensions.endOfMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth

object PaymentMapper {

    fun insert(payment: Payment): Int =
        session.run(
            queryOf(
                """
            INSERT INTO payment(
                date_time,
                chat_id,
                pupil_id,
                amount,
                services,
                photo_report
            ) VALUES (
                :dateTime,
                :chatId,
                :clientId,
                :amount,
                :serviceId,
                :photo_report
            ) RETURNING id
            """,
                mapOf(
                    "dateTime" to payment.dateTime,
                    "chatId" to payment.chatId,
                    "clientId" to payment.clientId,
                    "amount" to payment.amount,
                    "serviceId" to payment.serviceId,
                    "photo_report" to payment.photoReport,
                ),
            )
                .map { row -> row.int("id") }
                .asSingle
        )!!

    fun setDelete(id: Int, isDelete: Boolean) =
        session.run(
            queryOf(
                """
            UPDATE payment
            SET is_deleted = :is_delete
            WHERE id = :id
            """,
                mapOf("id" to id, "is_delete" to isDelete),
            )
                .asUpdate
        )

    fun getPaymentsSumByClient(
        chatId: Long,
        serviceId: Long,
        clientId: Int,
        date: LocalDateTime,
    ): Long =
        session.run(
            queryOf(
                """
            SELECT sum(amount) as sum
            FROM payment
            WHERE chat_id = :chat_id
                AND services = :service_id
                AND pupil_id = :client_id
                AND date_time between :startDate and :endDate
                AND photo_report IS NOT NULL
                AND is_deleted = false
            """,
                mapOf(
                    "chat_id" to chatId,
                    "service_id" to serviceId,
                    "client_id" to clientId,
                    "startDate" to date.startOfMonth(),
                    "endDate" to date.endOfMonth(),
                ),
            )
                .map { row -> row.longOrNull("sum") }
                .asSingle
        ) ?: 0

    fun getPaymentsSum(chatId: Long, serviceId: Long, date: LocalDateTime): Long =
        session.run(
            queryOf(
                """
            SELECT sum(amount) as sum
            FROM payment
            WHERE chat_id = :chat_id
                AND services = :service_id
                AND date_time between :startDate and :endDate
                AND is_deleted = false
            """,
                mapOf(
                    "chat_id" to chatId,
                    "service_id" to serviceId,
                    "startDate" to date.startOfMonth(),
                    "endDate" to date.endOfMonth(),
                ),
            )
                .map { row -> row.longOrNull("sum") }
                .asSingle
        ) ?: 0

    private fun getPayments(
        chatId: Long,
        serviceId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ) = session.run(
        queryOf(
        """
        SELECT p.id,
               p.date_time,
               p.chat_id,
               p.pupil_id,
               p.amount,
               p.services,
               p.is_deleted,
               p.photo_report
        FROM payment p
        WHERE p.chat_id = :chat_id
            AND p.services = :service_id
            AND p.date_time between :start_date and :end_date
            AND p.is_deleted=false
    """, mapOf(
                "chat_id" to chatId,
                "service_id" to serviceId,
                "start_date" to startDate,
                "end_date" to endDate)
    ).map { it.parsePayment() }.asList)

    fun getCurrentMonthPayments(chatId: Long, serviceId: Long) = getPayments(
        chatId,
        serviceId,
        LocalDateTime.now().startOfMonth(),
        LocalDateTime.now().endOfMonth(),
    )

    fun getPrevMonthPayments(chatId: Long, serviceId: Long) = getPayments(
        chatId,
        serviceId,
        LocalDateTime.now().prevMonth().startOfMonth(),
        LocalDateTime.now().prevMonth().endOfMonth(),
    )

}
