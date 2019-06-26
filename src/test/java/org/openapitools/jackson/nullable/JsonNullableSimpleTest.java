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
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public final class JsonNullableSimpleTest {

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
    }

    @Test
    public void get() {
        JsonNullable<String> test = JsonNullable.of("hello");
        assertTrue(test.isPresent());
        assertEquals("hello", test.get());
    }

    @Test
    public void getMissing() {
        JsonNullable<String> test = JsonNullable.undefined();
        assertFalse(test.isPresent());
        try {
            test.get();
            fail("should have thrown exception");
        } catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
        }
    }

    @Test
    public void orElse() {
        JsonNullable<String> test = JsonNullable.of("hello");
        assertTrue(test.isPresent());
        assertEquals("hello", test.orElse("world"));
    }

    @Test
    public void orElseMissing() {
        JsonNullable<String> test = JsonNullable.undefined();
        assertFalse(test.isPresent());
        assertEquals("world", test.orElse("world"));
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
    public void deserializeStringMembers() throws IOException {
        testReadPetName(JsonNullable.of("Rex"), "{\"name\":\"Rex\"}");
        testReadPetName(JsonNullable.<String>of(null), "{\"name\":null}");
        testReadPetName(JsonNullable.<String>of(""), "{\"name\":\"\"}");
        testReadPetName(JsonNullable.<String>of("   "), "{\"name\":\"   \"}");
        testReadPetName(JsonNullable.<String>undefined(), "{}");
    }

    @Test
    public void deserializeNonStringMembers() throws IOException {
        testReadPetAge(JsonNullable.of(Integer.valueOf(15)), "{\"age\":\"15\"}");
        testReadPetAge(JsonNullable.<Integer>of(null), "{\"age\":null}");
        testReadPetAge(JsonNullable.<Integer>undefined(), "{\"age\":\"\"}");
        testReadPetAge(JsonNullable.<Integer>undefined(), "{\"age\":\"   \"}");
        testReadPetAge(JsonNullable.<Integer>undefined(), "{}");
    }

    @Test
    public void deserializeStringNonBeanMembers() throws IOException {
        assertEquals(JsonNullable.of(null), mapper.readValue("null", new TypeReference<JsonNullable<String>>() {}));
        assertEquals(JsonNullable.of("42"), mapper.readValue("\"42\"", new TypeReference<JsonNullable<String>>() {}));
        assertEquals(JsonNullable.of(""), mapper.readValue("\"\"", new TypeReference<JsonNullable<String>>() {}));
        assertEquals(JsonNullable.of("   "), mapper.readValue("\"   \"", new TypeReference<JsonNullable<String>>() {}));
    }

    @Test
    public void deserializeNonStringNonBeanMembers() throws IOException {
        assertEquals(JsonNullable.of(null), mapper.readValue("\"null\"", new TypeReference<JsonNullable<Integer>>() {}));
        assertEquals(JsonNullable.of(42), mapper.readValue("\"42\"", new TypeReference<JsonNullable<Integer>>() {}));
        assertEquals(JsonNullable.undefined(), mapper.readValue("\"\"", new TypeReference<JsonNullable<Integer>>() {}));
        assertEquals(JsonNullable.undefined(), mapper.readValue("\"  \"", new TypeReference<JsonNullable<Integer>>() {}));
    }

    @Test
    public void deserializeCollection() throws IOException {
        List<JsonNullable<String>> values = mapper.readValue("[\"foo\", null]",
                new TypeReference<List<JsonNullable<String>>>() {
                });
        assertEquals(2, values.size());
        assertEquals(JsonNullable.of("foo"), values.get(0));
        assertEquals(JsonNullable.of(null), values.get(1));
    }

    private void testReadPetName(JsonNullable<String> expected, String json) throws IOException {
        Pet pet = mapper.readValue(json, Pet.class);
        JsonNullable<String> name = pet.name;
        assertEquals(expected, name);
    }

    private void testReadPetAge(JsonNullable<Integer> expected, String json) throws IOException {
        Pet pet = mapper.readValue(json, Pet.class);
        JsonNullable<Integer> age = pet.age;
        assertEquals(expected, age);
    }

    private static class Pet {

        public JsonNullable<String> name = JsonNullable.undefined();
        public JsonNullable<Integer> age = JsonNullable.undefined();

        public Pet name(JsonNullable<String> name) {
            this.name = name;
            return this;
        }

        public Pet age(JsonNullable<Integer> age) {
            this.age = age;
            return this;
        }
    }
}
