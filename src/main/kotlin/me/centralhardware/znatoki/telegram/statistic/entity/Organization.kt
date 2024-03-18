package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs
import me.centralhardware.znatoki.telegram.statistic.toCustomProperties
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

fun Row.parseOrganization() = Organization(
    uuid("id"),
    string("name"),
    long("owner"),
    long("log_chat_id"),
    string("service_custom_properties").toCustomProperties(),
    string("client_custom_properties").toCustomProperties(),
    string("payment_custom_properties").toCustomProperties(),
    string("grafana_username"),
    string("grafana_password"),
    string("grafana_url"),
    string("client_name")
)