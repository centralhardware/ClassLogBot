package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.parseLongList

class TelegramUser(
    val id: Long,
    val permissions: List<Permissions>,
    val services: List<Long>,
    val name: String,
)

fun Row.parseUser() =
    TelegramUser(
        long("id"),
        array<String>("permissions").map { Permissions.valueOf(it) },
        string("services").parseLongList(),
        string("name"),
    )
