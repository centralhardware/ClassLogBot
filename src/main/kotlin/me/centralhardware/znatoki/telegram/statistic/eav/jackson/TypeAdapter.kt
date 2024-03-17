package me.centralhardware.znatoki.telegram.statistic.eav.jackson

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.centralhardware.znatoki.telegram.statistic.eav.types.*

object TypeAdapter: KSerializer<Type>{
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)


    override fun deserialize(decoder: Decoder): Type {
        val value = decoder.decodeString()
        return when (value) {
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

    override fun serialize(encoder: Encoder, value: Type) {
        encoder.encodeString(value.name())
    }

}