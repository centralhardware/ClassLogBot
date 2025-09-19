package me.centralhardware.znatoki.telegram.statistic

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs

fun String.parseStringList() = split(":")

fun String.parseLongList() = split(":").map { it.toLong() }

fun String.toCustomProperties(): PropertyDefs = Json.decodeFromString(this.replace("\\", ""))

val json = Json { isLenient = true }

fun String.toProperties(): List<Property> = json.decodeFromString(this.replace("\\", ""))

fun List<Property>.toJson() = json.encodeToString(ListSerializer(Property.serializer()), this)
