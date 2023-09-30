package me.centralhardware.znatoki.telegram.statistic.eav.types;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class JacksonDeserializer extends JsonDeserializer<Type> {
    @Override
    public Type deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        var value = jsonParser.getValueAsString();
        return switch (value){
            case "DateTime" -> new DateTime();
            case "Enumeration" -> new Enumeration();
            case "Integer" -> new Integer();
            case "Photo" -> new Photo();
            case "Telephone" -> new Telephone();
            case "Text" -> new Text();
            default -> null;
        };
    }

}
