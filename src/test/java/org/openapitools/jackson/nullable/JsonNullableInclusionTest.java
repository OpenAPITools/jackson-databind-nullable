package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonNullableInclusionTest extends ModuleTestBase
{
    @JsonAutoDetect(fieldVisibility=Visibility.ANY)
    public static final class JsonNullableData {
        public JsonNullable<String> myString = JsonNullable.undefined();
    }

    // for [datatype-jdk8#18]
    static class JsonNullableNonEmptyStringBean {
        @JsonInclude(value=Include.NON_EMPTY, content=Include.NON_EMPTY)
        public JsonNullable<String> value;

        public JsonNullableNonEmptyStringBean() { }
        JsonNullableNonEmptyStringBean(String str) {
            value = JsonNullable.of(str);
        }
    }

    public static final class JsonNullableGenericData<T> {
        public JsonNullable<T> myData;
        public static <T> JsonNullableGenericData<T> construct(T data) {
            JsonNullableGenericData<T> ret = new JsonNullableGenericData<T>();
            ret.myData = JsonNullable.of(data);
            return ret;
        }
    }

    static final class OptMapBean {
        public Map<String, JsonNullable<?>> values;

        public OptMapBean(String key, JsonNullable<?> v) {
            values = new LinkedHashMap<String, JsonNullable<?>>();
            values.put(key, v);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = mapperWithModule();

    @Test
    void testSerOptNonEmpty() {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule(JsonInclude.Include.NON_EMPTY).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNonDefault() {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule(JsonInclude.Include.NON_DEFAULT).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNonAbsent() {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = mapperWithModule(JsonInclude.Include.NON_ABSENT).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testExcludeEmptyStringViaJsonNullable() {
        String json = MAPPER.writeValueAsString(new JsonNullableNonEmptyStringBean("x"));
        assertEquals("{\"value\":\"x\"}", json);
        json = MAPPER.writeValueAsString(new JsonNullableNonEmptyStringBean(null));
        assertEquals("{\"value\":null}", json);
        json = MAPPER.writeValueAsString(new JsonNullableNonEmptyStringBean(""));
        assertEquals("{}", json);
    }

    @Test
    void testSerPropInclusionAlways() {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS);
        ObjectMapper mapper = mapperBuilderWithModule().changeDefaultPropertyInclusion(incl_ -> incl).build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testSerPropInclusionNonNull() {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_NULL);
        ObjectMapper mapper = mapperBuilderWithModule().changeDefaultPropertyInclusion(incl_ -> incl).build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testSerPropInclusionNonAbsent() {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT);
        ObjectMapper mapper = mapperBuilderWithModule().changeDefaultPropertyInclusion(incl_ -> incl).build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testSerPropInclusionNonEmpty() {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_EMPTY);
        ObjectMapper mapper = mapperBuilderWithModule().changeDefaultPropertyInclusion(incl_ -> incl).build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testMapElementInclusion() {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_ABSENT);
        ObjectMapper mapper = mapperBuilderWithModule().changeDefaultPropertyInclusion(incl_ -> incl).build();
        // first: Absent entry/-ies should NOT be included
        assertEquals("{\"values\":{}}",
                mapper.writeValueAsString(new OptMapBean("key", JsonNullable.undefined())));
        // but non-empty should
        assertEquals("{\"values\":{\"key\":\"value\"}}",
                mapper.writeValueAsString(new OptMapBean("key", JsonNullable.of("value"))));
        // and actually even empty
        assertEquals("{\"values\":{\"key\":\"\"}}",
                mapper.writeValueAsString(new OptMapBean("key", JsonNullable.of(""))));
    }
}