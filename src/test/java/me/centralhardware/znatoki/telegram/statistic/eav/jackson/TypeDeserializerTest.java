package me.centralhardware.znatoki.telegram.statistic.eav.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import me.centralhardware.znatoki.telegram.statistic.eav.types.*;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Integer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TypeDeserializerTest {

    private final TypeDeserializer typeDeserializer = new TypeDeserializer();

    @Test
    void deserializeDay() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Date");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(Date.class, result);
    }

    @Test
    void deserializeDateTime() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("DateTime");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(DateTime.class, result);
    }

    @Test
    void deserializeEnumeration() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Enumeration");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(Enumeration.class, result);
    }

    @Test
    void deserializeNumber() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Integer");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(Integer.class, result);
    }

    @Test
    void deserializePhoto() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Photo");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(Photo.class, result);
    }

    @Test
    void deserializeTelephone() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Telephone");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(Telephone.class, result);
    }

    @Test
    void deserializeText() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Text");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertInstanceOf(Text.class, result);
    }

    @Test
    void deserializeUnrecognizedType() throws IOException {
        var jsonParser = mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn("Unrecognized");

        Type result = typeDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));

        assertNull(result);
    }
}