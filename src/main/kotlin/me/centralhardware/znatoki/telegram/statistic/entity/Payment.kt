package me.centralhardware.znatoki.telegram.statistic.entity

import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

data class Payment (
    val id: Int? = null,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val chatId: Long? = null,
    val clientId: Int,
    val amount: Int,
    val timeId: UUID? = null,
    val organizationId: UUID,
    val serviceId: Long? = null,
    val properties: MutableList<Property> = ArrayList()
)

class PaymentBuilder: Builder{
    var chatId by Delegates.notNull<Long>()
    var clientId by Delegates.notNull<Int>()
    var amount by Delegates.notNull<Int>()
    lateinit var organizationId: UUID
    var serviceId by Delegates.notNull<Long>()
    var properties: List<Property> = ArrayList()
    lateinit var propertiesBuilder: PropertiesBuilder

    fun nextProperty() = propertiesBuilder.next()

    fun build(): Payment = Payment(
        chatId = chatId,
        clientId = clientId,
        amount = amount,
        organizationId = organizationId,
        serviceId = serviceId,
        properties = propertiesBuilder.properties
    )

}