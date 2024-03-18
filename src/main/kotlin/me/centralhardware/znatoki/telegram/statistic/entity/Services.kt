package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import java.util.UUID

data class Services(
    var id: Long,
    var key: String,
    var name: String,
    var orgId: UUID,
    var allowMultiplyClients: Boolean,
    var properties: List<Property> = listOf()
)

fun Row.parseServices() = Services(
    long("id"),
    string("key"),
    string("name"),
    uuid("organization_id"),
    boolean("allow_multiply_clients")
)