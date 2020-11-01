package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonNullWithEmptyTest  extends ModuleTestBase
{
    private final ObjectMapper MAPPER = mapperWithModule();

    static class BooleanBean {
        public JsonNullable<Boolean> value;

        public BooleanBean() { }
        public BooleanBean(Boolean b) {
            value = JsonNullable.of(b);
        }
    }

    public void testJsonNullableFromEmpty() throws Exception {
        JsonNullable<?> value = MAPPER.readValue(quote(""), new TypeReference<JsonNullable<Integer>>() {});
        assertValueIsEmpty(value);
    }

    // for [datatype-jdk8#23]
    public void testBooleanWithEmpty() throws Exception
    {
        // and looks like a special, somewhat non-conforming case is what a user had
        // issues with
        BooleanBean b = MAPPER.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);

        assertValueIsEmpty(b.value);
    }

}