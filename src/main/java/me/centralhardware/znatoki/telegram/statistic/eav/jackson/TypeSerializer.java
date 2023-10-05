package me.centralhardware.znatoki.telegram.statistic.eav.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.centralhardware.znatoki.telegram.statistic.eav.types.*;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Number;

import java.io.IOException;

public class TypeSerializer extends JsonSerializer<Type> {
    @Override
    public void serialize(Type type, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        switch (type) {
            case Date v -> jsonGenerator.writeString("Date");
            case DateTime v -> jsonGenerator.writeString("DateTime");
            case Enumeration v -> jsonGenerator.writeString("Enumeration");
            case Number v -> jsonGenerator.writeString("Integer");
            case Photo v -> jsonGenerator.writeString("Photo");
            case Telephone v -> jsonGenerator.writeString("Telephone");
            case Text v -> jsonGenerator.writeString("Text");
        }
    }
}
