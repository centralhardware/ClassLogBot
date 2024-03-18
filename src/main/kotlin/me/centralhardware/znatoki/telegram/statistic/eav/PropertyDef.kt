package me.centralhardware.znatoki.telegram.statistic.eav

import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.eav.json.TypeAdapter
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type

@Serializable
data class PropertyDef(
    @Serializable(with = TypeAdapter::class)
    val type: Type,
    val name: String,
    val enumeration: List<String> = listOf(),
    val isOptional: Boolean,
    val isUnique: Boolean = false
)