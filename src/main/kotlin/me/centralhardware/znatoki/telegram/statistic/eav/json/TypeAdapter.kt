package me.centralhardware.znatoki.telegram.statistic.eav.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.centralhardware.znatoki.telegram.statistic.eav.types.*

object TypeAdapter: KSerializer<Type>{
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)


    override fun deserialize(decoder: Decoder): Type = decoder.decodeString().toType()

    override fun serialize(encoder: Encoder, value: Type) {
        encoder.encodeString(value.name())
    }

}