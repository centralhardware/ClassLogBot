package me.centralhardware.znatoki.telegram.statistic.entity

import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs
import java.util.*

data class Organization(
    val id: UUID,
    val name: String,
    val owner: Long,
    val logChatId: Long?,
    val serviceCustomProperties: PropertyDefs,
    val clientCustomProperties: PropertyDefs,
    val paymentCustomProperties: PropertyDefs,
    val grafanaUsername: String,
    val grafanaPassword: String,
    val grafanaUrl: String,
    val clientName: String
)