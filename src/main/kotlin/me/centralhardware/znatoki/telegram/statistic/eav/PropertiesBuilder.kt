package me.centralhardware.znatoki.telegram.statistic.eav

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.eav.types.EnumType
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.*

class PropertiesBuilder(propertyDefs: MutableList<PropertyDef>) {

    private val propertyDefs: LinkedList<PropertyDef> = LinkedList(propertyDefs)
    val properties: MutableList<Property> = ArrayList()
    private var current: PropertyDef

    init {
        current = propertyDefs.first()
        propertyDefs.removeFirst()
    }

    fun next(): Pair<String, List<String>>? {
        if (propertyDefs.isEmpty()) return null

        val res =  if (current.type is EnumType) {
            Pair(current.type.format(current.name, current.isOptional), current.enumeration)
        } else {
            Pair(current.type.format(current.name, current.isOptional), listOf())
        }

        current = propertyDefs.first()
        propertyDefs.remove(current)

        return res
    }

    fun validate(update: Update): Either<String, Unit> {
        return if (current.type is EnumType) {
            current.type.validate(update, *current.enumeration.toTypedArray())
        } else {
            current.type.validate(update)
        }
    }

    fun setProperty(value: Update): Boolean {
        return if (value.hasMessage() && Objects.equals(value.message.text, "/skip")) {
            properties.add(Property(current.name, current.type))
            true
        } else {
            current.type.extract(value)?.let {
                properties.add(Property(current.name, current.type, it))
            } == null
        }
    }
}