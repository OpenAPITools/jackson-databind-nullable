package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class JsonNullableBasicTest extends ModuleTestBase {

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

    public void testJsonNullableTypeResolution() throws Exception {
        // With 2.6, we need to recognize it as ReferenceType
        JavaType t = MAPPER.constructType(JsonNullable.class);
        assertNotNull(t);
        assertEquals(JsonNullable.class, t.getRawClass());
        assertTrue(t.isReferenceType());
    }

    public void testDeserAbsent() throws Exception {
        JsonNullable<?> value = MAPPER.readValue("null",
                new TypeReference<JsonNullable<String>>() {
                });
        assertNull(value.get());
    }

    public void testDeserSimpleString() throws Exception {
        JsonNullable<?> value = MAPPER.readValue("\"simpleString\"",
                new TypeReference<JsonNullable<String>>() {
                });
        assertTrue(value.isPresent());
        assertEquals("simpleString", value.get());
    }

    public void testDeserInsideObject() throws Exception {
        JsonNullableData data = MAPPER.readValue("{\"myString\":\"simpleString\"}",
                JsonNullableData.class);
        assertTrue(data.myString.isPresent());
        assertEquals("simpleString", data.myString.get());
    }

    public void testDeserComplexObject() throws Exception {
        TypeReference<JsonNullable<JsonNullableData>> type = new TypeReference<JsonNullable<JsonNullableData>>() {
        };
        JsonNullable<JsonNullableData> data = MAPPER.readValue(
                "{\"myString\":\"simpleString\"}", type);
        assertTrue(data.isPresent());
        assertTrue(data.get().myString.isPresent());
        assertEquals("simpleString", data.get().myString.get());
    }

    public void testDeserGeneric() throws Exception {
        TypeReference<JsonNullable<JsonNullableGenericData<String>>> type = new TypeReference<JsonNullable<JsonNullableGenericData<String>>>() {
        };
        JsonNullable<JsonNullableGenericData<String>> data = MAPPER.readValue(
                "{\"myData\":\"simpleString\"}", type);
        assertTrue(data.isPresent());
        assertTrue(data.get().myData.isPresent());
        assertEquals("simpleString", data.get().myData.get());
    }

    public void testSerAbsent() throws Exception {
        String value = MAPPER.writeValueAsString(JsonNullable.undefined());
        assertEquals("null", value);
    }

    public void testSerSimpleString() throws Exception {
        String value = MAPPER.writeValueAsString(JsonNullable.of("simpleString"));
        assertEquals("\"simpleString\"", value);
    }

    public void testSerInsideObject() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of("simpleString");
        String value = MAPPER.writeValueAsString(data);
        assertEquals("{\"myString\":\"simpleString\"}", value);
    }

    public void testSerComplexObject() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of("simpleString");
        String value = MAPPER.writeValueAsString(JsonNullable.of(data));
        assertEquals("{\"myString\":\"simpleString\"}", value);
    }

    public void testSerGeneric() throws Exception {
        JsonNullableGenericData<String> data = new JsonNullableGenericData<String>();
        data.myData = JsonNullable.of("simpleString");
        String value = MAPPER.writeValueAsString(JsonNullable.of(data));
        assertEquals("{\"myData\":\"simpleString\"}", value);
    }

    public void testSerOptDefault() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.undefined();
        String value = mapperWithModule().setSerializationInclusion(
                JsonInclude.Include.ALWAYS).writeValueAsString(data);
        assertEquals("{}", value);
    }

    public void testSerOptNull() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule().setSerializationInclusion(
                JsonInclude.Include.NON_NULL).writeValueAsString(data);
        assertEquals("{}", value);
    }

    public void testSerOptNullNulled() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of(null);
        String value = mapperWithModule().setSerializationInclusion(
                JsonInclude.Include.NON_NULL).writeValueAsString(data);
        assertEquals("{\"myString\":null}", value);
    }

    public void testSerOptAbsent() throws Exception {
        final JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.undefined();

        ObjectMapper mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        assertEquals("{}", mapper.writeValueAsString(data));

        // but do exclude with NON_EMPTY
        mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        assertEquals("{}", mapper.writeValueAsString(data));

        // and with new (2.6) NON_ABSENT
        mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals("{}", mapper.writeValueAsString(data));
    }

    public void testSerOptAbsentNull() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = JsonNullable.of(null);
        String value = mapperWithModule().setSerializationInclusion(
                JsonInclude.Include.NON_ABSENT).writeValueAsString(data);
        assertEquals("{\"myString\":null}", value);
    }

    public void testSerOptNonEmpty() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule().setSerializationInclusion(
                JsonInclude.Include.NON_EMPTY).writeValueAsString(data);
        assertEquals("{}", value);
    }

    public void testWithTypingEnabled() throws Exception {
        final ObjectMapper objectMapper = mapperWithModule();
        // ENABLE TYPING
        objectMapper
                .enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);

        final JsonNullableData myData = new JsonNullableData();
        myData.myString = JsonNullable.of("abc");

        final String json = objectMapper.writeValueAsString(myData);
        final JsonNullableData deserializedMyData = objectMapper.readValue(json,
                JsonNullableData.class);
        assertEquals(myData.myString, deserializedMyData.myString);
    }

    public void testObjectId() throws Exception {
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

    public void testJsonNullableCollection() throws Exception {

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
            assertEquals("Entry #" + i, list.get(i), result.get(i));
        }
    }

    public void testDeserNull() throws Exception {
        JsonNullable<?> value = MAPPER.readValue("\"\"", new TypeReference<JsonNullable<Integer>>() {});
        assertFalse(value.isPresent());
    }

    public void testPolymorphic() throws Exception
    {
        final Container dto = new Container();
        dto.contained = JsonNullable.of((Contained) new ContainedImpl());

        final String json = MAPPER.writeValueAsString(dto);

        final Container fromJson = MAPPER.readValue(json, Container.class);
        assertNotNull(fromJson.contained);
        assertTrue(fromJson.contained.isPresent());
        assertSame(ContainedImpl.class, fromJson.contained.get().getClass());
    }
}