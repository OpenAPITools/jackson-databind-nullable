package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

// TODO: fix JsonNullable in constructor annotated by JsonCreator
@Disabled("JsonNullable in a constructor is deserialized to JsonNullable[null] instead of JsonNullable.undefined")
class CreatorTest extends ModuleTestBase {
    static class CreatorWithJsonNullableStrings {
        JsonNullable<String> a, b;

        // note: something weird with test setup, should not need annotations
        @JsonCreator
        public CreatorWithJsonNullableStrings(@JsonProperty("a") JsonNullable<String> a,
                                              @JsonProperty("b") JsonNullable<String> b) {
            this.a = a;
            this.b = b;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    /**
     * Test to ensure that creator parameters use defaulting
     * (introduced in Jackson 2.6)
     */
    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void testCreatorWithJsonNullable(JsonProcessor jsonProcessor) throws Exception {
        jsonProcessor.mapperWithModule();
        CreatorWithJsonNullableStrings bean = jsonProcessor.readValue(
                aposToQuotes("{'a':'foo'}"), CreatorWithJsonNullableStrings.class);
        assertNotNull(bean);
        assertNotNull(bean.a);
        assertNotNull(bean.b);
        assertTrue(bean.a.isPresent());
        assertFalse(bean.b.isPresent());
        assertEquals("foo", bean.a.get());
    }
}