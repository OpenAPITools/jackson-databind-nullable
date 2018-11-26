package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public final class JsonNullableSimpleTest {

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
    }

    @Test
    public void serializeNonBeanProperty() throws JsonProcessingException {
        assertEquals("null", mapper.writeValueAsString(JsonNullable.of(null)));
        assertEquals("\"foo\"", mapper.writeValueAsString(JsonNullable.of("foo")));
        // TODO: Serialize non bean JsonNullable.undefined to empty string
        assertEquals("null", mapper.writeValueAsString(JsonNullable.undefined()));

    }

    @Test
    public void serializeAlways() throws JsonProcessingException {
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    public void serializeNonNull() throws JsonProcessingException {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    public void serializeNonAbsent() throws JsonProcessingException {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    public void serializeCollection() throws JsonProcessingException {
        assertEquals("[\"foo\",null,null,null]", mapper.writeValueAsString(Arrays.asList(
                JsonNullable.of("foo"),
                JsonNullable.of(null),
                JsonNullable.<String>undefined(),
                null
        )));
    }

    @Test
    public void deserialize() throws IOException {
        testReadPetName(JsonNullable.of("Rex"), "{\"name\":\"Rex\"}");
        testReadPetName(JsonNullable.<String>of(null), "{\"name\":null}");
        testReadPetName(JsonNullable.<String>undefined(), "{}");
    }

    @Test
    public void deserializeNonBeanProperty() throws IOException {
        assertEquals(JsonNullable.of(null), mapper.readValue("\"null\"", new TypeReference<JsonNullable<Integer>>() {}));
        assertEquals(JsonNullable.of(42), mapper.readValue("\"42\"", new TypeReference<JsonNullable<Integer>>() {}));
        assertEquals(JsonNullable.undefined(), mapper.readValue("\"\"", new TypeReference<JsonNullable<Integer>>() {}));
    }

    @Test
    public void deserializeCollection() throws IOException {
        List<JsonNullable<String>> values = mapper.readValue("[\"foo\", null]", new TypeReference<List<JsonNullable<String>>>() {});
        assertEquals(2, values.size());
        assertEquals(JsonNullable.of("foo"), values.get(0));
        assertEquals(JsonNullable.of(null), values.get(1));
    }

    private void testReadPetName(JsonNullable<String> expected, String json) throws IOException {
        Pet pet = mapper.readValue(json, Pet.class);
        JsonNullable<String> name = pet.getName();
        assertEquals(expected, name);
    }

    private static class Pet {

        private JsonNullable<String> name = JsonNullable.undefined();

        public JsonNullable<String> getName() {
            return name;
        }

        public Pet name(JsonNullable<String> name) {
            this.name = name;
            return this;
        }
    }
}
