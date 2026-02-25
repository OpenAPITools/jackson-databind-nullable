package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ParameterizedClass
@MethodSource("jsonProcessors")
class JsonNullableInclusionTest extends ModuleTestBase {
    @JsonAutoDetect(fieldVisibility = Visibility.ANY)
    public static final class JsonNullableData {
        public JsonNullable<String> myString = JsonNullable.undefined();
    }

    // for [datatype-jdk8#18]
    static class JsonNullableNonEmptyStringBean {
        @JsonInclude(value = Include.NON_EMPTY, content = Include.NON_EMPTY)
        public JsonNullable<String> value;

        public JsonNullableNonEmptyStringBean() {
        }

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

    @Parameter
    JsonProcessor jsonProcessor;

    @BeforeEach
    void setUp() {
        jsonProcessor.mapperWithModule();
    }

    @Test
    void testSerOptNonEmpty() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = jsonProcessor.setDefaultPropertyInclusion(
                JsonInclude.Include.NON_EMPTY).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNonDefault() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = jsonProcessor.setDefaultPropertyInclusion(
                JsonInclude.Include.NON_DEFAULT).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testSerOptNonAbsent() throws Exception {
        JsonNullableData data = new JsonNullableData();
        data.myString = null;
        String value = jsonProcessor.setDefaultPropertyInclusion(
                JsonInclude.Include.NON_ABSENT).writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    void testExcludeEmptyStringViaJsonNullable() throws Exception {
        String json = jsonProcessor.writeValueAsString(new JsonNullableNonEmptyStringBean("x"));
        assertEquals("{\"value\":\"x\"}", json);
        json = jsonProcessor.writeValueAsString(new JsonNullableNonEmptyStringBean(null));
        assertEquals("{\"value\":null}", json);
        json = jsonProcessor.writeValueAsString(new JsonNullableNonEmptyStringBean(""));
        assertEquals("{}", json);
    }

    @Test
    void testSerPropInclusionAlways() throws Exception {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS);
        jsonProcessor.setDefaultPropertyInclusion(incl);
        assertEquals("{\"myData\":true}",
                jsonProcessor.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testSerPropInclusionNonNull() throws Exception {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_NULL);
        jsonProcessor.setDefaultPropertyInclusion(incl);
        assertEquals("{\"myData\":true}",
                jsonProcessor.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testSerPropInclusionNonAbsent() throws Exception {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT);
        jsonProcessor.setDefaultPropertyInclusion(incl);
        assertEquals("{\"myData\":true}",
                jsonProcessor.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testSerPropInclusionNonEmpty() throws Exception {
        JsonInclude.Value incl =
                JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_EMPTY);
        jsonProcessor.setDefaultPropertyInclusion(incl);
        assertEquals("{\"myData\":true}",
                jsonProcessor.writeValueAsString(JsonNullableGenericData.construct(Boolean.TRUE)));
    }

    @Test
    void testMapElementInclusion() throws Exception {
        jsonProcessor.setDefaultPropertyInclusion(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_ABSENT));
        // first: Absent entry/-ies should NOT be included
        assertEquals("{\"values\":{}}",
                jsonProcessor.writeValueAsString(new OptMapBean("key", JsonNullable.undefined())));
        // but non-empty should
        assertEquals("{\"values\":{\"key\":\"value\"}}",
                jsonProcessor.writeValueAsString(new OptMapBean("key", JsonNullable.of("value"))));
        // and actually even empty
        assertEquals("{\"values\":{\"key\":\"\"}}",
                jsonProcessor.writeValueAsString(new OptMapBean("key", JsonNullable.of(""))));
    }
}
