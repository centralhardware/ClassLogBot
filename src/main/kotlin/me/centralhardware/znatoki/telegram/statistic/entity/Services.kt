package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row

data class Services(
    var id: Long,
    var name: String,
    var allowMultiplyClients: Boolean
)

fun Row.parseServices() = Services(long("id"), string("name"), boolean("allow_multiply_clients"))
