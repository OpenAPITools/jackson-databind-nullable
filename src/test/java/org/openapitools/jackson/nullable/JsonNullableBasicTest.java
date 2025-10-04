package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonNullableBasicTest extends ModuleTestBase {

    public static final class JsonNullableData {
        public JsonNullable<String> myString;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class JsonNullableGenericData<T> {
        private JsonNullable<T> myData;
    }

    @JsonIdentityInfo(generator= ObjectIdGenerators.IntSequenceGenerator.class)
    public static class Unit
    {
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
    public static interface Contained { }

    public static class ContainedImpl implements Contained { }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = mapperWithModule();

    @Test
    void testJsonNullableTypeResolution() {
        // With 2.6, we need to recognize it as ReferenceType
        JavaType t = MAPPER.constructType(JsonNullable.class);
        assertNotNull(t);
        assertEquals(JsonNullable.class, t.getRawClass());
        assertTrue(t.isReferenceType());
    }

    @Test
    void testDeserAbsent() {
        JsonNullable<?> value = MAPPER.readValue("null",
                new TypeReference<JsonNullable<String>>() {
                });
        assertNull(value.get());
    }

    @Test
    void testDeserSimpleString() {
        JsonNullable<?> value = MAPPER.readValue("\"simpleString\"",
                new TypeReference<JsonNullable<String>>() {
                });
        assertTrue(value.isPresent());
        assertEquals("simpleString", value.get());
    }

    @Test
    void testDeserInsideObject() {
        JsonNullableData data = MAPPER.readValue("{\"myString\":\"simpleString\"}",
                JsonNullableData.class);
        assertTrue(data.myString.isPresent());
        assertEquals("simpleString", data.myString.get());
    }

    @Test
    void testDeserComplexObject() {
        TypeReference<JsonNullable<JsonNullableData>> type = new TypeReference<JsonNullable<JsonNullableData>>() {
        };
        JsonNullable<JsonNullableData> data = MAPPER.readValue(
                "{\"myString\":\"simpleString\"}", type);
        assertTrue(data.isPresent());
        assertTrue(data.get().myString.isPresent());
        assertEquals("simpleString", data.get().myString.get());
    }

    @Test
    void testDeserGeneric() {
        TypeReference<JsonNullable<JsonNullableGenericData<String>>> type = new TypeReference<JsonNullable<JsonNullableGenericData<String>>>() {
        };
        JsonNullable<JsonNullableGenericData<String>> data = MAPPER.readValue(
                "{\"myData\":\"simpleString\"}", type);
        assertTrue(data.isPresent());
        assertTrue(data.get().myData.isPresent());
        assertEquals("simpleString", data.get().myData.get());
    }

    @Test
    void testSerAbsent() {
        String value = MAPPER.writeValueAsString(JsonNullable.undefined());
        assertEquals("null", value);
    }

    @Test
    void testSerSimpleString() {
        String value = MAPPER.writeValueAsString(JsonNullable.of("simpleString"));
        assertEquals("\"simpleString\"", value);
    }

    @Test
    void testSerInsideObject() {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of("simpleString");
        String value = MAPPER.writeValueAsString(data);
        assertEquals("{\"myString\":\"simpleString\"}", value);
    }

    @Test
    void testSerComplexObject() {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of("simpleString");
        String value = MAPPER.writeValueAsString(JsonNullable.of(data));
        assertEquals("{\"myString\":\"simpleString\"}", value);
    }

    @Test
    void testSerGeneric() {
        JsonNullableGenericData<String> data = new JsonNullableGenericData<String>();
        data.myData = JsonNullable.of("simpleString");
        String value = MAPPER.writeValueAsString(JsonNullable.of(data));
        assertEquals("{\"myData\":\"simpleString\"}", value);
    }

    @Test
    void testSerOptDefault() {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.undefined();
        String value = mapperWithModule(JsonInclude.Include.ALWAYS).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNull() {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule(JsonInclude.Include.NON_NULL).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNullNulled() {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of(null);
        String value = mapperWithModule(
                JsonInclude.Include.NON_NULL).writeValueAsString(data);
        assertEquals("{\"myString\":null}", value);
    }

    @Test
    void testSerOptAbsent() {
        final JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.undefined();

        ObjectMapper mapper = mapperWithModule(JsonInclude.Include.NON_NULL);

        assertEquals("{}", mapper.writeValueAsString(data));

        // but do exclude with NON_EMPTY
        mapper = mapperWithModule(JsonInclude.Include.NON_EMPTY);
        assertEquals("{}", mapper.writeValueAsString(data));

        // and with new (2.6) NON_ABSENT
        mapper = mapperWithModule(JsonInclude.Include.NON_ABSENT);
        assertEquals("{}", mapper.writeValueAsString(data));
    }

    @Test
    void testSerOptAbsentNull() {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of(null);
        String value = mapperWithModule(
                JsonInclude.Include.NON_ABSENT).writeValueAsString(data);
        assertEquals("{\"myString\":null}", value);
    }

    @Test
    void testSerOptNonEmpty() {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule(
                JsonInclude.Include.NON_EMPTY).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testWithTypingEnabled() {
        final class AllowAllValidator extends PolymorphicTypeValidator.Base
        {
            public AllowAllValidator() {}

            @Override
            public Validity validateBaseType(DatabindContext ctxt, JavaType baseType) {
                return Validity.INDETERMINATE;
            }

            @Override
            public Validity validateSubClassName(DatabindContext ctxt,
                    JavaType baseType, String subClassName) {
                return Validity.ALLOWED;
            }

            @Override
            public Validity validateSubType(DatabindContext ctxt, JavaType baseType,
                    JavaType subType) {
                return Validity.ALLOWED;
            }
        }

        // ENABLE TYPING
        final ObjectMapper objectMapper = mapperBuilderWithModule().activateDefaultTyping(new AllowAllValidator(), DefaultTyping.OBJECT_AND_NON_CONCRETE).build();

        final JsonNullableData myData = new JsonNullableData();
        myData.myString = JsonNullable.of("abc");

        final String json = objectMapper.writeValueAsString(myData);
        final JsonNullableData deserializedMyData = objectMapper.readValue(json,
                JsonNullableData.class);
        assertEquals(myData.myString, deserializedMyData.myString);
    }

    @Test
    void testObjectId() {
        final Unit input = new Unit();
        input.link(input);
        String json = MAPPER.writeValueAsString(input);
        Unit result = MAPPER.readValue(json, Unit.class);
        assertNotNull(result);
        assertNotNull(result.baseUnit);
        assertTrue(result.baseUnit.isPresent());
        Unit base = result.baseUnit.get();
        assertSame(result, base);
    }

    @Test
    void testJsonNullableCollection() {

        TypeReference<List<JsonNullable<String>>> typeReference = new TypeReference<List<JsonNullable<String>>>() {
        };

        List<JsonNullable<String>> list = new ArrayList<JsonNullable<String>>();
        list.add(JsonNullable.of("2014-1-22"));
        list.add(JsonNullable.<String>of(null));
        list.add(JsonNullable.of("2014-1-23"));

        String str = MAPPER.writeValueAsString(list);
        assertEquals("[\"2014-1-22\",null,\"2014-1-23\"]", str);

        List<JsonNullable<String>> result = MAPPER.readValue(str, typeReference);
        assertEquals(list.size(), result.size());
        for (int i = 0; i < list.size(); ++i) {
            assertEquals(list.get(i), result.get(i),"Entry #" + i);
        }
    }

    @Test
    void testDeserNull() {
        JsonNullable<?> value = MAPPER.readValue("\"\"", new TypeReference<JsonNullable<Integer>>() {});
        assertFalse(value.isPresent());
    }

    @Test
    void testPolymorphic() {
        final Container dto = new Container();
        dto.contained = JsonNullable.of((Contained) new ContainedImpl());

        final String json = MAPPER.writeValueAsString(dto);

        final Container fromJson = MAPPER.readValue(json, Container.class);
        assertNotNull(fromJson.contained);
        assertTrue(fromJson.contained.isPresent());
        assertSame(ContainedImpl.class, fromJson.contained.get().getClass());
    }
}