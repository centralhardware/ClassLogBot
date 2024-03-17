package me.centralhardware.znatoki.telegram.statistic.eav

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.eav.jackson.JacksonTypeAdapter
import me.centralhardware.znatoki.telegram.statistic.eav.jackson.TypeAdapter
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type

@Serializable
data class Property(
    val name: String,
    @Serializable(with = TypeAdapter::class)
    @JsonSerialize(using = JacksonTypeAdapter.Serializer::class)
    @JsonDeserialize(using = JacksonTypeAdapter.Deserializator::class)
    val type: Type,
    var value: String? = ""
) {
    fun withValue(value: String) = this.apply { this.value = value }
}