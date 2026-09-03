package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class CreatorTest extends ModuleTestBase {
    static class CreatorWithJsonNullableStrings {
        JsonNullable<String> a, b;

        // note: parameter names are not retained by default, hence the explicit @JsonProperty
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
        assertEquals(JsonNullable.<String>undefined(), bean.b);
    }

    /**
     * An absent creator property has to stay distinguishable from one that is
     * explicitly set to <code>null</code>, which is the reason JsonNullable exists.
     */
    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void testCreatorSeparatesExplicitNullFromAbsent(JsonProcessor jsonProcessor) throws Exception {
        jsonProcessor.mapperWithModule();
        CreatorWithJsonNullableStrings bean = jsonProcessor.readValue(
                aposToQuotes("{'a':null}"), CreatorWithJsonNullableStrings.class);
        assertNotNull(bean);
        assertEquals(JsonNullable.<String>of(null), bean.a);
        assertEquals(JsonNullable.<String>undefined(), bean.b);
    }
}
