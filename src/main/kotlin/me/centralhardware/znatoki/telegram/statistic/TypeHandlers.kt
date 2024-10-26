package me.centralhardware.znatoki.telegram.statistic

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs
import me.centralhardware.znatoki.telegram.statistic.entity.Role

fun String.toRole(): Role = enumValueOf(this)

fun String.parseStringList() = split(":")

fun String.parseLongList() = split(":").map { it.toLong() }

fun String.toCustomProperties(): PropertyDefs = Json.decodeFromString(this)

fun String.toProperties(): List<Property> = Json { isLenient = true }.decodeFromString(this)

fun List<Property>.toJson() = Json.encodeToString(ListSerializer(Property.serializer()), this)
