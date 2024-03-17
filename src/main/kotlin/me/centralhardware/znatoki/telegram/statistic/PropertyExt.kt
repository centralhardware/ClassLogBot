package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo
import me.centralhardware.znatoki.telegram.statistic.eav.types.Telephone

private fun Property.applyTypeFormat(): Property {
    return if (this.type is Telephone) {
        this.value.formatTelephone()?.let { this.withValue(it) }?: this
    } else {
        this
    }
}

fun List<Property>.print(): String {
    return this
        .filterNot { it.type is Photo }
        .map { it.applyTypeFormat() }
        .joinToString("\n") { property -> "${property.name}=${property.value.makeBold()}" } ?: ""
}