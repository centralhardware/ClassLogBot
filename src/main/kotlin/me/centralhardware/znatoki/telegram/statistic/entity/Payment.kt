package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import java.time.LocalDateTime
import java.util.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.toProperties

data class Payment(
    val id: Int? = null,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val chatId: Long,
    val clientId: Int,
    val amount: Int,
    val serviceId: Long,
    val deleted: Boolean = false,
    val properties: List<Property> = ArrayList(),
)

fun Row.parsePayment() = Payment(
    int("id"),
    localDateTime("date_time"),
    long("chat_id"),
    int("pupil_id"),
    int("amount"),
    long("services"),
    boolean("is_deleted"),
    string("properties").toProperties()
)

class PaymentBuilder : Builder {
    var chatId: Long? = null
    var clientId: Int? = null
    var amount: Int? = null
    var serviceId: Long? = null
    var properties: List<Property>? = null
    var propertiesBuilder: PropertiesBuilder? = null

    fun nextProperty() = propertiesBuilder!!.next()

    fun build(): Payment =
        Payment(
            chatId = chatId!!,
            clientId = clientId!!,
            amount = amount!!,
            serviceId = serviceId!!,
            properties = propertiesBuilder!!.properties,
        )
}
