package me.centralhardware.znatoki.telegram.statistic.eav

import kotlinx.serialization.Serializable

@Serializable
data class PropertyDefs(val propertyDefs: List<PropertyDef>) {
    fun isEmpty() = propertyDefs.isEmpty()
}