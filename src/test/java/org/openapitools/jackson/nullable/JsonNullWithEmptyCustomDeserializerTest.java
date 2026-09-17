package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// for [openapi-generator/jackson-databind-nullable#46]: a user-registered (SimpleModule)
// deserializer for the wrapped type must see the empty string itself instead of this
// library shortcutting it to undefined() first.
//
// Not parameterized over JsonProcessor like JsonNullWithEmptyTest: each stack needs its own
// SimpleModule/ObjectMapper wiring, so each test method below already targets one specific
// stack and there is nothing left for a shared jsonProcessor field to add. Kept in a dedicated
// class (rather than @Test methods on the @ParameterizedClass one) so each runs once, not once
// per JsonProcessor.
class JsonNullWithEmptyCustomDeserializerTest extends ModuleTestBase {

    static class CustomDeserializedBean {
        public String note;

        static final CustomDeserializedBean EMPTY_SENTINEL = new CustomDeserializedBean();
        static {
            EMPTY_SENTINEL.note = "sentinel-for-empty-string";
        }
    }

    static class CustomDeserializedBeanBox {
        public JsonNullable<CustomDeserializedBean> value;
    }

    static class CustomDeserializedBeanJackson2Deserializer extends JsonDeserializer<CustomDeserializedBean> {
        @Override
        public CustomDeserializedBean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING && p.getText().isEmpty()) {
                return CustomDeserializedBean.EMPTY_SENTINEL;
            }
            p.skipChildren();
            return new CustomDeserializedBean();
        }
    }

    static class CustomDeserializedBeanJackson3Deserializer extends tools.jackson.databind.ValueDeserializer<CustomDeserializedBean> {
        @Override
        public CustomDeserializedBean deserialize(tools.jackson.core.JsonParser p, tools.jackson.databind.DeserializationContext ctxt) {
            if (p.currentToken() == tools.jackson.core.JsonToken.VALUE_STRING && p.getString().isEmpty()) {
                return CustomDeserializedBean.EMPTY_SENTINEL;
            }
            p.skipChildren();
            return new CustomDeserializedBean();
        }
    }

    @Test
    void testJackson2CustomDeserializerReceivesEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        SimpleModule custom = new SimpleModule();
        custom.addDeserializer(CustomDeserializedBean.class, new CustomDeserializedBeanJackson2Deserializer());
        mapper.registerModule(custom);

        CustomDeserializedBeanBox box = mapper.readValue(aposToQuotes("{'value':''}"), CustomDeserializedBeanBox.class);
        assertTrue(box.value.isPresent());
        assertSame(CustomDeserializedBean.EMPTY_SENTINEL, box.value.get());
    }

    // Jackson 3 twin of the test above.
    @Test
    void testJackson3CustomDeserializerReceivesEmptyString() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.addDeserializer(CustomDeserializedBean.class, new CustomDeserializedBeanJackson3Deserializer());
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(custom)
                .build();

        CustomDeserializedBeanBox box = mapper.readValue(aposToQuotes("{'value':''}"), CustomDeserializedBeanBox.class);
        assertTrue(box.value.isPresent());
        assertSame(CustomDeserializedBean.EMPTY_SENTINEL, box.value.get());
    }
}
