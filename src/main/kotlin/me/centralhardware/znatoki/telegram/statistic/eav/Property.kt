package me.centralhardware.znatoki.telegram.statistic.eav

import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.eav.json.TypeAdapter
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type

@Serializable
data class Property(
    val name: String,
    @Serializable(with = TypeAdapter::class) val type: Type,
    var value: String? = "",
) {
    fun withValue(value: String) = this.apply { this.value = value }
}
