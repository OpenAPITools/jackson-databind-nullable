package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// TODO: fix JsonNullable in constructor annotated by JsonCreator
@Disabled("JsonNullable in a constructor is deserialized to JsonNullable[null] instead of JsonNullable.undefined")
class CreatorTest extends ModuleTestBase
{
    static class CreatorWithJsonNullableStrings
    {
        JsonNullable<String> a, b;

        // note: something weird with test setup, should not need annotations
        @JsonCreator
        public CreatorWithJsonNullableStrings(@JsonProperty("a") JsonNullable<String> a,
                                          @JsonProperty("b") JsonNullable<String> b)
        {
            this.a = a;
            this.b = b;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = mapperWithModule();

    /**
     * Test to ensure that creator parameters use defaulting
     * (introduced in Jackson 2.6)
     */
    @Test
    void testCreatorWithJsonNullable() throws Exception
    {
        CreatorWithJsonNullableStrings bean = MAPPER.readValue(
                aposToQuotes("{'a':'foo'}"), CreatorWithJsonNullableStrings.class);
        assertNotNull(bean);
        assertNotNull(bean.a);
        assertNotNull(bean.b);
        assertTrue(bean.a.isPresent());
        assertFalse(bean.b.isPresent());
        assertEquals("foo", bean.a.get());
    }
}