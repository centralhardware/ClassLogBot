package me.centralhardware.znatoki.telegram.statistic.eav

import arrow.core.Either
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import me.centralhardware.znatoki.telegram.statistic.eav.types.EnumType
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

        current = propertyDefs.first()
        propertyDefs.removeFirst()

        val res =  if (current.type is EnumType) {
            Pair(current.type.format(current.name, current.isOptional), current.enumeration)
        } else {
            Pair(current.type.format(current.name, current.isOptional), listOf())
        }

        return res
    }

    fun validate(message: CommonMessage<MessageContent>): Either<String, Unit> {
        return if (current.type is EnumType) {
            current.type.validate(message, *current.enumeration.toTypedArray())
        } else {
            current.type.validate(message)
        }
    }

    fun setProperty(value: CommonMessage<MessageContent>): Boolean {
        return if (value.content is TextContent && value.content.asTextContent()!!.text == "/skip") {
            properties.add(Property(current.name, current.type))
            true
        } else {
            current.type.extract(value)?.let {
                properties.add(Property(current.name, current.type, it))
            } == null
        }
    }
}