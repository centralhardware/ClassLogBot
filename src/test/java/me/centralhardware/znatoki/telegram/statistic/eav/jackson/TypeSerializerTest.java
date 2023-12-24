package me.centralhardware.znatoki.telegram.statistic.eav.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.centralhardware.znatoki.telegram.statistic.eav.types.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class TypeSerializerTest {

    /**
     * This class test serialize method of class TypeSerializer.
     * The serialize method is expected to write string representation of type name in the JsonGenerator.
     */
     
    @Test
    public void testSerialize() throws IOException {
        // Initialize objects
        JsonGenerator jsonGenerator = Mockito.mock(JsonGenerator.class);
        SerializerProvider serializerProvider = Mockito.mock(SerializerProvider.class);
        Type type = Mockito.mock(Type.class);

        // Set up mock behaviors
        Mockito.when(type.getName()).thenReturn("TestName");

        // Create instance of class under test
        TypeSerializer typeSerializer = new TypeSerializer();

        // Call method under test
        typeSerializer.serialize(type, jsonGenerator, serializerProvider);

        // Verify behavior
        Mockito.verify(jsonGenerator).writeString("TestName");

        // Assert that no exception is thrown
        Assertions.assertDoesNotThrow(() -> typeSerializer.serialize(type, jsonGenerator, serializerProvider));
    }
}