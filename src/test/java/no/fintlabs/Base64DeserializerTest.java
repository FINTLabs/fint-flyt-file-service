package no.fintlabs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class Base64DeserializerTest {

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    private Base64Deserializer base64Deserializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        base64Deserializer = new Base64Deserializer();
    }

    @Test
    void testDeserialize() throws IOException {
        String encodedString = Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8));
        when(jsonParser.getValueAsString()).thenReturn(encodedString);

        byte[] result = base64Deserializer.deserialize(jsonParser, deserializationContext);

        assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void testDeserializeEmptyString() throws IOException {
        when(jsonParser.getValueAsString()).thenReturn("");

        byte[] result = base64Deserializer.deserialize(jsonParser, deserializationContext);

        assertArrayEquals(new byte[0], result);
    }

    @Test
    void testDeserializeNullString() throws IOException {
        when(jsonParser.getValueAsString()).thenReturn(null);

        assertThrows(NullPointerException.class, () ->
                base64Deserializer.deserialize(jsonParser, deserializationContext)
        );
    }

    @Test
    void testDeserializeInvalidBase64() throws IOException {
        when(jsonParser.getValueAsString()).thenReturn("thisIsNotValidBase64==");

        assertThrows(IllegalArgumentException.class, () ->
                base64Deserializer.deserialize(jsonParser, deserializationContext)
        );
    }

}
