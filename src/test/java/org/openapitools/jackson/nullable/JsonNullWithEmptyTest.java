package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@MethodSource("jsonProcessors")
class JsonNullWithEmptyTest extends ModuleTestBase {

    @Parameter
    JsonProcessor jsonProcessor;

    static class BooleanBean {
        public JsonNullable<Boolean> value;

        public BooleanBean() {
        }

        public BooleanBean(Boolean b) {
            value = JsonNullable.of(b);
        }
    }

    enum Color { RED, GREEN }

    static class EnumBean {
        public JsonNullable<Color> value;
    }

    static class Point {
        public int x;
        public int y;
    }

    static class PojoBean {
        public JsonNullable<Point> value;
    }

    @BeforeEach
    void setup() {
        jsonProcessor.mapperWithModule();
    }

    @Test
    void testJsonNullableFromEmpty() throws Exception {
        JsonNullable<?> value = jsonProcessor.readValue(quote(""), TypeReferences.INTEGER.getType(jsonProcessor));
        assertFalse(value.isPresent());
    }

    // for [datatype-jdk8#23]
    @Test
    void testBooleanWithEmpty() throws Exception {
        // and looks like a special, somewhat non-conforming case is what a user had
        // issues with
        BooleanBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);

        assertFalse(b.value.isPresent());
    }

    // mapBlankStringToNull(true): blank strings for non-String targets become a present null

    @Test
    void testJsonNullableFromEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        JsonNullable<?> value = jsonProcessor.readValue(quote(""), TypeReferences.INTEGER.getType(jsonProcessor));
        assertTrue(value.isPresent());
        assertNull(value.get());
    }

    @Test
    void testJsonNullableFromBlankWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        JsonNullable<?> value = jsonProcessor.readValue(quote("   "), TypeReferences.INTEGER.getType(jsonProcessor));
        assertTrue(value.isPresent());
        assertNull(value.get());
    }

    @Test
    void testBooleanWithEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        BooleanBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }

    // The guard runs before the content deserializer, so enum and POJO targets never
    // see the blank string. Without it these would throw instead of yielding a present null.
    @Test
    void testEnumWithEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        EnumBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), EnumBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }

    @Test
    void testPojoWithEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        PojoBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), PojoBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }

    @Test
    void testStringTargetUnaffectedByMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        JsonNullable<?> value = jsonProcessor.readValue(quote(""), TypeReferences.STRING.getType(jsonProcessor));
        assertTrue(value.isPresent());
        assertEquals("", value.get());
    }

    private enum TypeReferences {
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
        };

        public abstract Object getType(JsonProcessor jsonProcessor);
    }

}