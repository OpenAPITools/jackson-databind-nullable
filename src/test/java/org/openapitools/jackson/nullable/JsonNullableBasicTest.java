package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass
@MethodSource("jsonProcessors")
class JsonNullableBasicTest extends ModuleTestBase {

    public static final class JsonNullableData {
        public JsonNullable<String> myString;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class JsonNullableGenericData<T> {
        private JsonNullable<T> myData;
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
        JSON_NULLABLE_DATA {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<JsonNullableData>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<JsonNullableData>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        },
        JSON_NULLABLE_GENERIC_DATA {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<JsonNullableGenericData<String>>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<JsonNullableGenericData<String>>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        },
        LIST_JSON_NULLABLE_STRING {
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

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static class Unit {
        public JsonNullable<Unit> baseUnit;

        public Unit() {
        }

        public Unit(final JsonNullable<Unit> u) {
            baseUnit = u;
        }

        public void link(final Unit u) {
            baseUnit = JsonNullable.of(u);
        }
    }

    // To test handling of polymorphic value types

    public static class Container {
        public JsonNullable<Contained> contained;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "ContainedImpl", value = ContainedImpl.class),
    })
    public interface Contained {
    }

    public static class ContainedImpl implements Contained {
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Parameter
    JsonProcessor jsonProcessor;

    @BeforeEach
    void setUp() {
        jsonProcessor.mapperWithModule();
    }

    @Test
    void testJsonNullableTypeResolution() {
        // With 2.6, we need to recognize it as ReferenceType
        JsonProcessor.TypeDescriptor t = jsonProcessor.constructType(JsonNullable.class);
        assertNotNull(t);
        assertEquals(JsonNullable.class, t.getRawClass());
        assertTrue(t.isReferenceType());
    }

    @Test
    void testDeserAbsent() throws Exception {
        Object type = TypeReferences.STRING.getType(jsonProcessor);
        JsonNullable<?> value = jsonProcessor.readValue("null",
                type);
        assertNull(value.get());
    }

    @Test
    void testDeserSimpleString() throws Exception {
        Object type = TypeReferences.STRING.getType(jsonProcessor);
        JsonNullable<?> value = jsonProcessor.readValue("\"simpleString\"",
                type);
        assertTrue(value.isPresent());
        assertEquals("simpleString", value.get());
    }

    @Test
    void testDeserInsideObject() throws Exception {
        JsonNullableData data = jsonProcessor.readValue("{\"myString\":\"simpleString\"}",
                JsonNullableData.class);
        assertTrue(data.myString.isPresent());
        assertEquals("simpleString", data.myString.get());
    }

    @Test
    void testDeserComplexObject() throws Exception {
        Object type = TypeReferences.JSON_NULLABLE_DATA.getType(jsonProcessor);
        JsonNullable<JsonNullableData> data = jsonProcessor.readValue(
                "{\"myString\":\"simpleString\"}", type);
        assertTrue(data.isPresent());
        assertTrue(data.get().myString.isPresent());
        assertEquals("simpleString", data.get().myString.get());
    }

    @Test
    void testDeserGeneric() throws Exception {
        Object type = TypeReferences.JSON_NULLABLE_GENERIC_DATA.getType(jsonProcessor);
        JsonNullable<JsonNullableGenericData<String>> data = jsonProcessor.readValue(
                "{\"myData\":\"simpleString\"}", type);
        assertTrue(data.isPresent());
        assertTrue(data.get().myData.isPresent());
        assertEquals("simpleString", data.get().myData.get());
    }

    @Test
    void testSerAbsent() throws Exception {
        String value = jsonProcessor.writeValueAsString(JsonNullable.undefined());
        assertEquals("null", value);
    }

    @Test
    void testSerSimpleString() throws Exception {
        String value = jsonProcessor.writeValueAsString(JsonNullable.of("simpleString"));
        assertEquals("\"simpleString\"", value);
    }

    @Test
    void testSerInsideObject() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of("simpleString");
        String value = jsonProcessor.writeValueAsString(data);
        assertEquals("{\"myString\":\"simpleString\"}", value);
    }

    @Test
    void testSerComplexObject() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of("simpleString");
        String value = jsonProcessor.writeValueAsString(JsonNullable.of(data));
        assertEquals("{\"myString\":\"simpleString\"}", value);
    }

    @Test
    void testSerGeneric() throws Exception {
        JsonNullableGenericData<String> data = new JsonNullableGenericData<String>();
        data.myData = JsonNullable.of("simpleString");
        String value = jsonProcessor.writeValueAsString(JsonNullable.of(data));
        assertEquals("{\"myData\":\"simpleString\"}", value);
    }

    @Test
    void testSerOptDefault() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.undefined();
        String value = jsonProcessor.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNull() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = jsonProcessor.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNullNulled() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of(null);
        String value = jsonProcessor.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(data);
        assertEquals("{\"myString\":null}", value);
    }

    @Test
    void testSerOptAbsent() throws Exception {
        final JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.undefined();

        assertEquals("{}", jsonProcessor
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(data));

        // but do exclude with NON_EMPTY
        assertEquals("{}", jsonProcessor
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .writeValueAsString(data));

        // and with new (2.6) NON_ABSENT
        assertEquals("{}", jsonProcessor
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)
                .writeValueAsString(data));
    }

    @Test
    void testSerOptAbsentNull() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of(null);
        String value = jsonProcessor
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)
                .writeValueAsString(data);
        assertEquals("{\"myString\":null}", value);
    }

    @Test
    void testSerOptNonEmpty() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = jsonProcessor
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testWithTypingEnabled() throws Exception {
        jsonProcessor.objectAndNonConcreteTyping();
        final JsonNullableData myData = new JsonNullableData();
        myData.myString = JsonNullable.of("abc");

        final String json = jsonProcessor.writeValueAsString(myData);
        final JsonNullableData deserializedMyData = jsonProcessor.readValue(json,
                JsonNullableData.class);
        assertEquals(myData.myString, deserializedMyData.myString);
    }

    @Test
    void testObjectId() throws Exception {
        final Unit input = new Unit();
        input.link(input);
        String json = jsonProcessor.writeValueAsString(input);
        Unit result = jsonProcessor.readValue(json, Unit.class);
        assertNotNull(result);
        assertNotNull(result.baseUnit);
        assertTrue(result.baseUnit.isPresent());
        Unit base = result.baseUnit.get();
        assertSame(result, base);
    }

    @Test
    void testJsonNullableCollection() throws Exception {

        Object typeReference = TypeReferences.LIST_JSON_NULLABLE_STRING.getType(jsonProcessor);

        List<JsonNullable<String>> list = new ArrayList<JsonNullable<String>>();
        list.add(JsonNullable.of("2014-1-22"));
        list.add(JsonNullable.<String>of(null));
        list.add(JsonNullable.of("2014-1-23"));

        String str = jsonProcessor.writeValueAsString(list);
        assertEquals("[\"2014-1-22\",null,\"2014-1-23\"]", str);

        List<JsonNullable<String>> result = jsonProcessor.readValue(str, typeReference);
        assertEquals(list.size(), result.size());
        for (int i = 0; i < list.size(); ++i) {
            assertEquals(list.get(i), result.get(i), "Entry #" + i);
        }
    }

    @Test
    void testDeserNull() throws Exception {
        JsonNullable<?> value = jsonProcessor.readValue("\"\"", TypeReferences.INTEGER.getType(jsonProcessor));
        assertFalse(value.isPresent());
    }

    @Test
    void testPolymorphic() throws Exception {
        final Container dto = new Container();
        dto.contained = JsonNullable.of((Contained) new ContainedImpl());

        final String json = jsonProcessor.writeValueAsString(dto);

        final Container fromJson = jsonProcessor.readValue(json, Container.class);
        assertNotNull(fromJson.contained);
        assertTrue(fromJson.contained.isPresent());
        assertSame(ContainedImpl.class, fromJson.contained.get().getClass());
    }
}
