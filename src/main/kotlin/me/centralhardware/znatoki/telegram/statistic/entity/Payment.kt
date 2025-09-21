package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import java.time.LocalDateTime

 data class Payment(
    val id: Int? = null,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val chatId: Long,
    val clientId: Int,
    val amount: Int,
    val serviceId: Long,
    val deleted: Boolean = false,
    val photoReport: String? = null,
)

fun Row.parsePayment() = Payment(
    int("id"),
    localDateTime("date_time"),
    long("chat_id"),
    int("pupil_id"),
    int("amount"),
    long("services"),
    boolean("is_deleted"),
    stringOrNull("photo_report")
)

class PaymentBuilder : Builder {
    var chatId: Long? = null
    var clientId: Int? = null
    var amount: Int? = null
    var serviceId: Long? = null
    var photoReport: String? = null

    fun build(): Payment =
        Payment(
            chatId = chatId!!,
            clientId = clientId!!,
            amount = amount!!,
            serviceId = serviceId!!,
            photoReport = photoReport,
        )
}
