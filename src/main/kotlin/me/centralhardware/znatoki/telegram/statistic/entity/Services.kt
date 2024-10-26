package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.eav.Property

data class Services(
    var id: Long,
    var key: String,
    var name: String,
    var allowMultiplyClients: Boolean,
    var properties: List<Property> = listOf()
)

fun Row.parseServices() =
    Services(long("id"), string("key"), string("name"), boolean("allow_multiply_clients"))
