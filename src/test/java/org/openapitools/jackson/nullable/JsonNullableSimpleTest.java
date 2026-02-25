package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class JsonNullableSimpleTest {

    static Stream<JsonProcessor> jsonProcessors() {
        return Stream.of(
                new Jackson2Processor().mapperWithModule(),
                new Jackson3Processor().mapperWithModule()
        );
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

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void serializeNonBeanProperty(JsonProcessor jsonProcessor) throws Exception {
        assertEquals("null", jsonProcessor.writeValueAsString(JsonNullable.of(null)));
        assertEquals("\"foo\"", jsonProcessor.writeValueAsString(JsonNullable.of("foo")));
        // TODO: Serialize non bean JsonNullable.undefined to empty string
        assertEquals("null", jsonProcessor.writeValueAsString(JsonNullable.undefined()));

    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void serializeAlways(JsonProcessor jsonProcessor) throws Exception {
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{\"name\":null}", jsonProcessor.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void serializeNonNull(JsonProcessor jsonProcessor) throws Exception {
        jsonProcessor.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void serializeNonAbsent(JsonProcessor jsonProcessor) throws Exception {
        jsonProcessor.setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(null)));
        assertEquals("{\"name\":null}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}", jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void serializeCollection(JsonProcessor jsonProcessor) throws Exception {
        assertEquals("[\"foo\",null,null,null]", jsonProcessor.writeValueAsString(Arrays.asList(
                JsonNullable.of("foo"),
                JsonNullable.of(null),
                JsonNullable.<String>undefined(),
                null
        )));
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void deserializeStringMembers(JsonProcessor jsonProcessor) throws Exception {
        testReadPetName(jsonProcessor, JsonNullable.of("Rex"), "{\"name\":\"Rex\"}");
        testReadPetName(jsonProcessor, JsonNullable.<String>of(null), "{\"name\":null}");
        testReadPetName(jsonProcessor, JsonNullable.<String>of(""), "{\"name\":\"\"}");
        testReadPetName(jsonProcessor, JsonNullable.<String>of("   "), "{\"name\":\"   \"}");
        testReadPetName(jsonProcessor, JsonNullable.<String>undefined(), "{}");
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void deserializeNonStringMembers(JsonProcessor jsonProcessor) throws Exception {
        testReadPetAge(jsonProcessor, JsonNullable.of(Integer.valueOf(15)), "{\"age\":\"15\"}");
        testReadPetAge(jsonProcessor, JsonNullable.<Integer>of(null), "{\"age\":null}");
        testReadPetAge(jsonProcessor, JsonNullable.<Integer>undefined(), "{\"age\":\"\"}");
        testReadPetAge(jsonProcessor, JsonNullable.<Integer>undefined(), "{\"age\":\"   \"}");
        testReadPetAge(jsonProcessor, JsonNullable.<Integer>undefined(), "{}");
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void deserializeStringNonBeanMembers(JsonProcessor jsonProcessor) throws Exception {
        assertEquals(JsonNullable.of(null), jsonProcessor.readValue("null", TypeReferences.STRING.getType(jsonProcessor)));
        assertEquals(JsonNullable.of("42"), jsonProcessor.readValue("\"42\"", TypeReferences.STRING.getType(jsonProcessor)));
        assertEquals(JsonNullable.of(""), jsonProcessor.readValue("\"\"", TypeReferences.STRING.getType(jsonProcessor)));
        assertEquals(JsonNullable.of("   "), jsonProcessor.readValue("\"   \"", TypeReferences.STRING.getType(jsonProcessor)));
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void deserializeNonStringNonBeanMembers(JsonProcessor jsonProcessor) throws Exception {
        assertEquals(JsonNullable.of(null), jsonProcessor.readValue("\"null\"", TypeReferences.INTEGER.getType(jsonProcessor)));
        assertEquals(JsonNullable.of(42), jsonProcessor.readValue("\"42\"", TypeReferences.INTEGER.getType(jsonProcessor)));
        assertEquals(JsonNullable.undefined(), jsonProcessor.readValue("\"\"", TypeReferences.INTEGER.getType(jsonProcessor)));
        assertEquals(JsonNullable.undefined(), jsonProcessor.readValue("\"  \"", TypeReferences.INTEGER.getType(jsonProcessor)));
    }

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void deserializeCollection(JsonProcessor jsonProcessor) throws Exception {
        List<JsonNullable<String>> values = jsonProcessor.readValue("[\"foo\", null]",
                TypeReferences.LIST_NULLABLE_STRING.getType(jsonProcessor));
        assertEquals(2, values.size());
        assertEquals(JsonNullable.of("foo"), values.get(0));
        assertEquals(JsonNullable.of(null), values.get(1));
    }

    private void testReadPetName(JsonProcessor jsonProcessor, JsonNullable<String> expected, String json) throws Exception {
        Pet pet = jsonProcessor.readValue(json, Pet.class);
        JsonNullable<String> name = pet.name;
        assertEquals(expected, name);
    }

    private void testReadPetAge(JsonProcessor jsonProcessor, JsonNullable<Integer> expected, String json) throws Exception {
        Pet pet = jsonProcessor.readValue(json, Pet.class);
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

    private enum TypeReferences {
        STRING {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<String>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<String>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        },
        INTEGER {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<Integer>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<Integer>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        },
        LIST_NULLABLE_STRING {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<List<JsonNullable<String>>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<List<JsonNullable<String>>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        };

        public abstract Object getType(JsonProcessor jsonProcessor);
    }
}
