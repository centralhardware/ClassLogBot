package me.centralhardware.znatoki.telegram.statistic.eav.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.centralhardware.znatoki.telegram.statistic.eav.types.*;

import java.io.IOException;

public class TypeSerializer extends JsonSerializer<Type> {
    @Override
    public void serialize(Type type, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(type.getName());
    }
}
