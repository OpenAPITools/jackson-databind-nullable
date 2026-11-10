package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the opt-in suppression of {@code null} values during serialization,
 * configured via {@link JsonNullableModule#allowSuppressNulls(boolean)}
 * (see <a href="https://github.com/OpenAPITools/jackson-databind-nullable/issues/51">#51</a>).
 *
 * <p>With the flag off (default) the output must be identical to the behavior before
 * the option existed. With the flag on, {@code JsonNullable}-typed properties holding
 * {@code null} values ({@code of(null)}, {@code ofNull()}, {@code undefined()}, or a
 * plain {@code null} reference) are omitted from the output entirely, while
 * deserialization and non-bean serialization (root values, collection elements,
 * {@code Map} values) are unaffected.
 */
@ParameterizedClass
@MethodSource("jsonProcessors")
class JsonNullableSuppressNullsTest extends ModuleTestBase {

    static class Pet {
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

    static class MixedBean {
        public JsonNullable<String> name = JsonNullable.undefined();
        public String kind = null;
    }

    // for NON_EMPTY content inclusion interplay
    static class JsonNullableNonEmptyStringBean {
        @JsonInclude(value = Include.NON_EMPTY, content = Include.NON_EMPTY)
        public JsonNullable<String> value;

        public JsonNullableNonEmptyStringBean() {
        }

        JsonNullableNonEmptyStringBean(String str) {
            value = JsonNullable.of(str);
        }
    }

    static class OptMapBean {
        public Map<String, JsonNullable<?>> values;

        OptMapBean(String key, JsonNullable<?> v) {
            values = new LinkedHashMap<String, JsonNullable<?>>();
            values.put(key, v);
        }
    }

    @Parameter
    JsonProcessor jsonProcessor;

    /*
    /**********************************************************
    /* Flag off (default): output identical to pre-existing behavior
    /**********************************************************
     */

    @Test
    void testFlagOffDefaultInclusion() throws Exception {
        jsonProcessor.mapperWithModule();
        // undefined() omitted
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet()));
        // present-but-null written as explicit null
        assertEquals("{\"name\":null}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":null}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>ofNull())));
        // plain null reference written as null
        assertEquals("{\"name\":null}", jsonProcessor.writeValueAsString(new Pet().name(null)));
        // non-null values unaffected
        assertEquals("{\"name\":\"Rex\"}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
        assertEquals("{\"age\":3}",
                jsonProcessor.writeValueAsString(new Pet().age(JsonNullable.of(3))));
    }

    @Test
    void testFlagOffNonNullInclusion() throws Exception {
        jsonProcessor.mapperWithModule();
        jsonProcessor.setDefaultPropertyInclusion(Include.NON_NULL);
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet()));
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(null)));
        // present-but-null is still written as explicit null, not suppressed
        assertEquals("{\"name\":null}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    void testFlagOffNonBean() throws Exception {
        jsonProcessor.mapperWithModule();
        assertEquals("null", jsonProcessor.writeValueAsString(JsonNullable.<String>of(null)));
        assertEquals("[\"foo\",null,null]", jsonProcessor.writeValueAsString(Arrays.asList(
                JsonNullable.of("foo"),
                JsonNullable.<String>of(null),
                JsonNullable.<String>undefined())));
        assertEquals("{\"values\":{\"key\":null}}",
                jsonProcessor.writeValueAsString(new OptMapBean("key", JsonNullable.of(null))));
    }

    /*
    /**********************************************************
    /* Flag on: null-valued JsonNullable properties omitted
    /**********************************************************
     */

    @Test
    void testFlagOnSuppressesNullValues() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        // undefined() omitted (as before)
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet()));
        // present-but-null omitted entirely
        assertEquals("{}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>ofNull())));
        // plain null reference omitted entirely
        assertEquals("{}", jsonProcessor.writeValueAsString(new Pet().name(null)));
        // non-null values unaffected
        assertEquals("{\"name\":\"Rex\"}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
        assertEquals("{\"age\":3}",
                jsonProcessor.writeValueAsString(new Pet().age(JsonNullable.of(3))));
        // mixed null and non-null: only the non-null fields written
        assertEquals("{\"age\":3}", jsonProcessor.writeValueAsString(
                new Pet().name(JsonNullable.<String>of(null)).age(JsonNullable.of(3))));
    }

    @Test
    void testFlagOnWithNonNullInclusion() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        jsonProcessor.setDefaultPropertyInclusion(Include.NON_NULL);
        assertEquals("{}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>ofNull())));
        assertEquals("{\"name\":\"Rex\"}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    void testFlagOnWithNonAbsentAndNonEmptyInclusion() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        jsonProcessor.setDefaultPropertyInclusion(Include.NON_ABSENT);
        assertEquals("{}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        jsonProcessor.mapperWithModule(false, true);
        jsonProcessor.setDefaultPropertyInclusion(Include.NON_EMPTY);
        assertEquals("{}",
                jsonProcessor.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
    }

    @Test
    void testFlagOnWithNonEmptyContentInclusion() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        assertEquals("{\"value\":\"x\"}",
                jsonProcessor.writeValueAsString(new JsonNullableNonEmptyStringBean("x")));
        // present-but-null omitted by the flag
        assertEquals("{}", jsonProcessor.writeValueAsString(new JsonNullableNonEmptyStringBean(null)));
        // empty string still suppressed by NON_EMPTY content inclusion, unchanged by the flag
        assertEquals("{}", jsonProcessor.writeValueAsString(new JsonNullableNonEmptyStringBean("")));
    }

    @Test
    void testFlagOnOnlyAffectsJsonNullableProperties() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        // plain (non-JsonNullable) null property keeps the standard Jackson behavior
        // under ALWAYS inclusion: written as explicit null
        assertEquals("{\"kind\":null}", jsonProcessor.writeValueAsString(new MixedBean()));
        // JsonNullable property of the same bean is omitted
        MixedBean bean = new MixedBean();
        bean.name = JsonNullable.<String>of(null);
        assertEquals("{\"kind\":null}", jsonProcessor.writeValueAsString(bean));
    }

    @Test
    void testFlagOnNonBeanUnchanged() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        // root values cannot be omitted; still written as null
        assertEquals("null", jsonProcessor.writeValueAsString(JsonNullable.<String>of(null)));
        // collection elements cannot be skipped; still written as null
        assertEquals("[\"foo\",null,null]", jsonProcessor.writeValueAsString(Arrays.asList(
                JsonNullable.of("foo"),
                JsonNullable.<String>of(null),
                JsonNullable.<String>undefined())));
        // map values cannot be skipped; still written as null
        assertEquals("{\"values\":{\"key\":null}}",
                jsonProcessor.writeValueAsString(new OptMapBean("key", JsonNullable.of(null))));
    }

    /*
    /**********************************************************
    /* Deserialization unaffected
    /**********************************************************
     */

    @Test
    void testDeserializationUnaffected() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        Pet pet = jsonProcessor.readValue("{\"name\":null}", Pet.class);
        assertEquals(JsonNullable.<String>of(null), pet.name);
        pet = jsonProcessor.readValue("{}", Pet.class);
        assertEquals(JsonNullable.<String>undefined(), pet.name);
        pet = jsonProcessor.readValue("{\"name\":\"Rex\"}", Pet.class);
        assertEquals(JsonNullable.of("Rex"), pet.name);
    }

    @Test
    void testRoundtripWithFlagOn() throws Exception {
        jsonProcessor.mapperWithModule(false, true);
        Pet input = new Pet().name(JsonNullable.<String>of(null)).age(JsonNullable.of(3));
        String json = jsonProcessor.writeValueAsString(input);
        assertEquals("{\"age\":3}", json);
        Pet output = jsonProcessor.readValue(json, Pet.class);
        // a suppressed field comes back as undefined; present values round-trip
        assertEquals(JsonNullable.<String>undefined(), output.name);
        assertEquals(JsonNullable.of(3), output.age);
    }
}
