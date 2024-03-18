package me.centralhardware.znatoki.telegram.statistic.eav.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import me.centralhardware.znatoki.telegram.statistic.eav.types.*

object JacksonTypeAdapter {

    object Serializer: JsonSerializer<Type>() {
        override fun serialize(value: Type, gen: JsonGenerator, sp: SerializerProvider) =
            gen.writeString(value.name())
    }

    object Deserializator: JsonDeserializer<Type>(){
        override fun deserialize(jsonParser: JsonParser, p1: DeserializationContext): Type =
            when (jsonParser.valueAsString) {
                "Date" -> Date()
                "DateTime" -> DateTime()
                "Enumeration" -> Enumeration()
                "Integer" -> Integer()
                "Photo" -> Photo()
                "Telephone" -> Telephone()
                "Text" -> Text()
                else -> throw IllegalArgumentException()
            }
    }

}