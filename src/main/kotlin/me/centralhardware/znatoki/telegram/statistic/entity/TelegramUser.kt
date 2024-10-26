package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.parseLongList
import me.centralhardware.znatoki.telegram.statistic.toRole

class TelegramUser(val id: Long, val role: Role, val services: List<Long>, val name: String)

fun Row.parseUser() =
    TelegramUser(
        long("id"),
        string("role").toRole(),
        string("services").parseLongList(),
        string("name")
    )
