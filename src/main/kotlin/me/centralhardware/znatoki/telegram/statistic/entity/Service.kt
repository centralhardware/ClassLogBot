package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.toProperties
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

class Service(
    val id: UUID,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val chatId: Long,
    val serviceId: Long,
    val clientId: Int,
    val amount: Int,
    val properties: List<Property>
)

fun Row.parseTime() = Service(
    uuid("id"),
    localDateTime("date_time"),
    long("chat_id"),
    long("service_id"),
    int("pupil_id"),
    int("amount"),
    string("properties").toProperties(),
)

class ServiceBuilder: Builder {
    lateinit var id: UUID
    var chatId by Delegates.notNull<Long>()
    var serviceId by Delegates.notNull<Long>()
    var amount by Delegates.notNull<Int>()
    var properties: List<Property> = listOf()
    lateinit var propertiesBuilder: PropertiesBuilder
    var clientIds: MutableSet<Int> = HashSet()

    fun serviceId() = runCatching { serviceId }.getOrNull()

    fun clientId(clientId: Int) = clientIds.add(clientId)

    fun nextProperty() = propertiesBuilder.next()

    fun build(): List<Service> = clientIds.map {
        Service(
            id = id,
            chatId = chatId,
            serviceId = serviceId,
            clientId = it,
            amount = amount,
            properties = propertiesBuilder.properties.toList()
        )
    }
}

fun Collection<Service>.toClientIds(): List<Int> = this.map { it.clientId }.toList()