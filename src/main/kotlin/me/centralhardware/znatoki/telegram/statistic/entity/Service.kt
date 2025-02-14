package me.centralhardware.znatoki.telegram.statistic.entity

import java.time.LocalDateTime
import java.util.*
import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.toProperties

class Service(
    val id: UUID,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val updateTime: LocalDateTime = LocalDateTime.now(),
    val chatId: Long,
    val serviceId: Long,
    val clientId: Int,
    private val _amount: Int,
    val forceGroup: Boolean = false,
    val extraHalfHour: Boolean = false,
    val properties: List<Property>,
    val deleted: Boolean = false,
) {
    val amount: Double
        get() = if (extraHalfHour) _amount * 1.5 else _amount * 1.0
}

fun Row.parseTime() =
    Service(
        uuid("id"),
        localDateTime("date_time"),
        localDateTime("update_time"),
        long("chat_id"),
        long("service_id"),
        int("pupil_id"),
        int("amount"),
        boolean("force_group"),
        boolean("extra_half_hour"),
        string("properties").toProperties(),
        boolean("is_deleted"),
    )

class ServiceBuilder : Builder {
    var id: UUID = UUID.randomUUID()
    var chatId: Long? = null
    var serviceId: Long? = null
    var amount: Int? = null
    var properties: List<Property>? = null
    var propertiesBuilder: PropertiesBuilder? = null
    var clientIds: MutableSet<Int> = mutableSetOf()

    fun nextProperty() = propertiesBuilder!!.next()

    fun build(): List<Service> =
        clientIds.map {
            Service(
                id = id,
                chatId = chatId!!,
                serviceId = serviceId!!,
                clientId = it,
                _amount = amount!!,
                properties = propertiesBuilder!!.properties.toList(),
            )
        }
}

fun Collection<Service>.toClientIds(): List<Int> = this.map { it.clientId }.toList()

fun Collection<Service>.isIndividual() = size == 1 && !first().forceGroup

fun Collection<Service>.isGroup() = size > 1 || (size == 1 && first().forceGroup)
