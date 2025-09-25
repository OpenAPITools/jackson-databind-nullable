package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

final class JsonNullableSimpleTest {

    private JsonMapper.Builder mapperBuilder;

    @BeforeEach
    void setup() {
        mapperBuilder = JsonMapper.builder().addModule(new JsonNullableModule());
    }

    @Test
    void get() {
        JsonNullable<String> test = JsonNullable.of("hello");
        assertTrue(test.isPresent());
        assertEquals("hello", test.get());
    }

    @Test
    void getMissing() {
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
    void orElse() {
        JsonNullable<String> test = JsonNullable.of("hello");
        assertTrue(test.isPresent());
        assertEquals("hello", test.orElse("world"));
    }

    @Test
    void orElseMissing() {
        JsonNullable<String> test = JsonNullable.undefined();
        assertFalse(test.isPresent());
        assertEquals("world", test.orElse("world"));
    }

    @Test
    void ifPresentWithValueNotPresent() {
        JsonNullable<String> test = JsonNullable.undefined();
        assertDoesNotThrow(() -> test.ifPresent(string -> {
            throw new RuntimeException();
        }));
    }

    @Test
    void ifPresentWithNullValuePresent() {
        JsonNullable<String> test = JsonNullable.of(null);
        assertThrows(RuntimeException.class, () -> test.ifPresent(string -> {
            throw new RuntimeException();
        }));
    }

    @Test
    void ifPresentWithNonNullValuePresent() {
        JsonNullable<String> test = JsonNullable.of("test");
        assertThrows(RuntimeException.class, () -> test.ifPresent(string -> {
            throw new RuntimeException();
        }));
    }

    @Test
    void serializeNonBeanProperty() {
        ObjectMapper mapper = mapperBuilder.build();
        assertEquals("null", mapper.writeValueAsString(JsonNullable.of(null)));
        assertEquals("\"foo\"", mapper.writeValueAsString(JsonNullable.of("foo")));
        // TODO: Serialize non bean JsonNullable.undefined to empty string
        assertEquals("null", mapper.writeValueAsString(JsonNullable.undefined()));

    }

    @Test
    void serializeAlways() {
        ObjectMapper mapper = mapperBuilder.build();
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    void serializeNonNull() {
        ObjectMapper mapper = mapperBuilder.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)).build();
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    void serializeNonAbsent() {
        ObjectMapper mapper = mapperBuilder.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT)).build();
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{}", mapper.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    void serializeCollection() {
        ObjectMapper mapper = mapperBuilder.build();
        assertEquals("[\"foo\",null,null,null]", mapper.writeValueAsString(Arrays.asList(
                JsonNullable.of("foo"),
                JsonNullable.of(null),
                JsonNullable.<String>undefined(),
                null
        )));
    }

    @Test
    void deserializeStringMembers() {
        testReadPetName(JsonNullable.of("Rex"), "{\"name\":\"Rex\"}");
        testReadPetName(JsonNullable.<String>of(null), "{\"name\":null}");
        testReadPetName(JsonNullable.<String>of(""), "{\"name\":\"\"}");
        testReadPetName(JsonNullable.<String>of("   "), "{\"name\":\"   \"}");
        testReadPetName(JsonNullable.<String>undefined(), "{}");
    }

    @Test
    void deserializeNonStringMembers() {
        testReadPetAge(JsonNullable.of(Integer.valueOf(15)), "{\"age\":\"15\"}");
        testReadPetAge(JsonNullable.<Integer>of(null), "{\"age\":null}");
        testReadPetAge(JsonNullable.<Integer>undefined(), "{\"age\":\"\"}");
        testReadPetAge(JsonNullable.<Integer>undefined(), "{\"age\":\"   \"}");
        testReadPetAge(JsonNullable.<Integer>undefined(), "{}");
    }

    @Test
    void deserializeStringNonBeanMembers() {
        ObjectMapper mapper = mapperBuilder.build();
        assertEquals(JsonNullable.of(null), mapper.readValue("null", new TypeReference<JsonNullable<String>>() {
        }));
        assertEquals(JsonNullable.of("42"), mapper.readValue("\"42\"", new TypeReference<JsonNullable<String>>() {
        }));
        assertEquals(JsonNullable.of(""), mapper.readValue("\"\"", new TypeReference<JsonNullable<String>>() {
        }));
        assertEquals(JsonNullable.of("   "), mapper.readValue("\"   \"", new TypeReference<JsonNullable<String>>() {
        }));
    }

    @Test
    void deserializeNonStringNonBeanMembers() {
        ObjectMapper mapper = mapperBuilder.build();
        assertEquals(JsonNullable.of(null), mapper.readValue("\"null\"", new TypeReference<JsonNullable<Integer>>() {
        }));
        assertEquals(JsonNullable.of(42), mapper.readValue("\"42\"", new TypeReference<JsonNullable<Integer>>() {
        }));
        assertEquals(JsonNullable.undefined(), mapper.readValue("\"\"", new TypeReference<JsonNullable<Integer>>() {
        }));
        assertEquals(JsonNullable.undefined(), mapper.readValue("\"  \"", new TypeReference<JsonNullable<Integer>>() {
        }));
    }

    @Test
    void deserializeCollection() {
        ObjectMapper mapper = mapperBuilder.build();
        List<JsonNullable<String>> values = mapper.readValue("[\"foo\", null]",
                new TypeReference<List<JsonNullable<String>>>() {
                });
        assertEquals(2, values.size());
        assertEquals(JsonNullable.of("foo"), values.get(0));
        assertEquals(JsonNullable.of(null), values.get(1));
    }

    private void testReadPetName(JsonNullable<String> expected, String json) {
        ObjectMapper mapper = mapperBuilder.build();
        Pet pet = mapper.readValue(json, Pet.class);
        JsonNullable<String> name = pet.name;
        assertEquals(expected, name);
    }

    private void testReadPetAge(JsonNullable<Integer> expected, String json) {
        ObjectMapper mapper = mapperBuilder.build();
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
