package me.centralhardware.znatoki.telegram.statistic.eav.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import me.centralhardware.znatoki.telegram.statistic.eav.types.*;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Number;

import java.io.IOException;

public class TypeDeserializer extends JsonDeserializer<Type> {

    @Override
    public Type deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        var value = jsonParser.getValueAsString();
        return switch (value){
            case "Date" -> new Date();
            case "DateTime" -> new DateTime();
            case "Enumeration" -> new Enumeration();
            case "Integer" -> new Number();
            case "Photo" -> new Photo();
            case "Telephone" -> new Telephone();
            case "Text" -> new Text();
            default -> null;
        };
    }

}
