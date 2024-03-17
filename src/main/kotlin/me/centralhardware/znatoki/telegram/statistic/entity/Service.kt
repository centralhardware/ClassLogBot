package me.centralhardware.znatoki.telegram.statistic.entity

import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
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
    val organizationId: UUID,
    val properties: List<Property>
)


class ServiceBuilder: Builder {
    lateinit var id: UUID
    var chatId by Delegates.notNull<Long>()
    var serviceId by Delegates.notNull<Long>()
    var amount by Delegates.notNull<Int>()
    lateinit var organizationId: UUID
    var properties: List<Property> = listOf()
    lateinit var propertiesBuilder: PropertiesBuilder
    var clientIds: MutableSet<Int> = HashSet()

    fun clientId(clientId: Int) = clientIds.add(clientId)

    fun nextProperty() = propertiesBuilder.next()

    fun build(): List<Service> = clientIds.map {
        Service(
            id = id,
            chatId = chatId,
            serviceId = serviceId,
            clientId = it,
            amount = amount,
            organizationId = organizationId,
            properties = propertiesBuilder.properties.toList()
        )
    }
}

fun List<Service>.toClientIds(): List<Int> = this.map { it.clientId }.toList()