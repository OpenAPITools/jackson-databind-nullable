package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonNullWithEmptyTest  extends ModuleTestBase
{
    private final ObjectMapper MAPPER = mapperWithModule();
    private final ObjectMapper MAPPER_BLANK_TO_NULL = mapperWithModule(new JsonNullableModule().mapBlankStringToNull(true));

    static class BooleanBean {
        public JsonNullable<Boolean> value;

        public BooleanBean() { }
        public BooleanBean(Boolean b) {
            value = JsonNullable.of(b);
        }
    }

    // default behavior (mapBlankStringToNull = false)

    @Test
    void testJsonNullableFromEmpty() throws Exception {
        JsonNullable<?> value = MAPPER.readValue(quote(""), new TypeReference<JsonNullable<Integer>>() {});
        assertFalse(value.isPresent());
    }

    // for [datatype-jdk8#23]
    @Test
    void testBooleanWithEmpty() throws Exception {
        // and looks like a special, somewhat non-conforming case is what a user had
        // issues with
        BooleanBean b = MAPPER.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);

        assertFalse(b.value.isPresent());
    }

    // mapBlankStringToNull = true

    @Test
    void testJsonNullableFromEmptyWithMapBlankStringToNull() throws Exception {
        JsonNullable<?> value = MAPPER_BLANK_TO_NULL.readValue(quote(""), new TypeReference<JsonNullable<Integer>>() {});
        assertTrue(value.isPresent());
        assertNull(value.get());
    }

    @Test
    void testJsonNullableFromBlankWithMapBlankStringToNull() throws Exception {
        JsonNullable<?> value = MAPPER_BLANK_TO_NULL.readValue(quote("   "), new TypeReference<JsonNullable<Integer>>() {});
        assertTrue(value.isPresent());
        assertNull(value.get());
    }

    @Test
    void testBooleanWithEmptyWithMapBlankStringToNull() throws Exception {
        BooleanBean b = MAPPER_BLANK_TO_NULL.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }
}
