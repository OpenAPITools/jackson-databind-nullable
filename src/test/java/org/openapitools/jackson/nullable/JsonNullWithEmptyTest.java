package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@MethodSource("jsonProcessors")
class JsonNullWithEmptyTest extends ModuleTestBase {

    @Parameter
    JsonProcessor jsonProcessor;

    static class BooleanBean {
        public JsonNullable<Boolean> value;

        public BooleanBean() {
        }

        public BooleanBean(Boolean b) {
            value = JsonNullable.of(b);
        }
    }

    enum Color { RED, GREEN }

    static class EnumBean {
        public JsonNullable<Color> value;
    }

    static class Point {
        public int x;
        public int y;
    }

    static class PojoBean {
        public JsonNullable<Point> value;
    }

    // for [openapi-generator/jackson-databind-nullable#26]
    static class CharacterBean {
        public JsonNullable<Character> value;
    }

    static class CharSequenceBean {
        public JsonNullable<CharSequence> value;
    }

    static class StringBuilderBean {
        public JsonNullable<StringBuilder> value;
    }

    static class PlainPojo {
        public String a;
    }

    static class PlainPojoBean {
        public JsonNullable<PlainPojo> value;
    }

    @BeforeEach
    void setup() {
        jsonProcessor.mapperWithModule();
    }

    @Test
    void testJsonNullableFromEmpty() throws Exception {
        JsonNullable<?> value = jsonProcessor.readValue(quote(""), TypeReferences.INTEGER.getType(jsonProcessor));
        assertFalse(value.isPresent());
    }

    // for [datatype-jdk8#23]
    @Test
    void testBooleanWithEmpty() throws Exception {
        // and looks like a special, somewhat non-conforming case is what a user had
        // issues with
        BooleanBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);

        assertFalse(b.value.isPresent());
    }

    // mapBlankStringToNull(true): blank strings for non-String targets become a present null

    @Test
    void testJsonNullableFromEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        JsonNullable<?> value = jsonProcessor.readValue(quote(""), TypeReferences.INTEGER.getType(jsonProcessor));
        assertTrue(value.isPresent());
        assertNull(value.get());
    }

    @Test
    void testJsonNullableFromBlankWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        JsonNullable<?> value = jsonProcessor.readValue(quote("   "), TypeReferences.INTEGER.getType(jsonProcessor));
        assertTrue(value.isPresent());
        assertNull(value.get());
    }

    @Test
    void testBooleanWithEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        BooleanBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }

    // The guard runs before the content deserializer, so enum and POJO targets never
    // see the blank string. Without it these would throw instead of yielding a present null.
    @Test
    void testEnumWithEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        EnumBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), EnumBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }

    @Test
    void testPojoWithEmptyWithMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        PojoBean b = jsonProcessor.readValue(aposToQuotes("{'value':''}"), PojoBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertNull(b.value.get());
    }

    @Test
    void testStringTargetUnaffectedByMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        JsonNullable<?> value = jsonProcessor.readValue(quote(""), TypeReferences.STRING.getType(jsonProcessor));
        assertTrue(value.isPresent());
        assertEquals("", value.get());
    }

    // for [openapi-generator/jackson-databind-nullable#26]: Character is string-like, so an
    // empty (or blank) string must reach Jackson's own CharacterDeserializer instead of being
    // shortcut to undefined(). Measured (not assumed): Jackson's CharacterDeserializer maps ""
    // to a present `null`, and a single-character string (including " ") to that character.
    @Test
    void testCharacterWithEmptyBlankAndValue() throws Exception {
        CharacterBean empty = jsonProcessor.readValue(aposToQuotes("{'value':''}"), CharacterBean.class);
        assertNotNull(empty.value);
        assertTrue(empty.value.isPresent());
        assertNull(empty.value.get());

        CharacterBean blank = jsonProcessor.readValue(aposToQuotes("{'value':' '}"), CharacterBean.class);
        assertTrue(blank.value.isPresent());
        assertEquals(Character.valueOf(' '), blank.value.get());

        CharacterBean withValue = jsonProcessor.readValue(aposToQuotes("{'value':'a'}"), CharacterBean.class);
        assertTrue(withValue.value.isPresent());
        assertEquals(Character.valueOf('a'), withValue.value.get());
    }

    // Interaction (neither side had this): Character is string-like, so isStringLike is true
    // and the guard - and therefore mapBlankStringToNull - never runs, regardless of the
    // option; the delegate CharacterDeserializer alone decides, exactly as measured above in
    // testCharacterWithEmptyBlankAndValue with the option off.
    @Test
    void testCharacterWithEmptyAndMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        CharacterBean bean = jsonProcessor.readValue(aposToQuotes("{'value':''}"), CharacterBean.class);
        assertNotNull(bean.value);
        assertTrue(bean.value.isPresent());
        assertNull(bean.value.get());
    }

    // for [openapi-generator/jackson-databind-nullable#26]: CharSequence (and its
    // implementations) must be treated like String, not shortcut to undefined().
    @Test
    void testCharSequenceWithEmptyBlankAndValue() throws Exception {
        CharSequenceBean empty = jsonProcessor.readValue(aposToQuotes("{'value':''}"), CharSequenceBean.class);
        assertTrue(empty.value.isPresent());
        assertEquals("", empty.value.get().toString());

        CharSequenceBean blank = jsonProcessor.readValue(aposToQuotes("{'value':' '}"), CharSequenceBean.class);
        assertTrue(blank.value.isPresent());
        assertEquals(" ", blank.value.get().toString());

        CharSequenceBean withValue = jsonProcessor.readValue(aposToQuotes("{'value':'a'}"), CharSequenceBean.class);
        assertTrue(withValue.value.isPresent());
        assertEquals("a", withValue.value.get().toString());
    }

    // for [openapi-generator/jackson-databind-nullable#26]: a concrete CharSequence
    // implementation must also be passed through rather than shortcut.
    @Test
    void testStringBuilderWithEmptyAndValue() throws Exception {
        StringBuilderBean empty = jsonProcessor.readValue(aposToQuotes("{'value':''}"), StringBuilderBean.class);
        assertTrue(empty.value.isPresent());
        assertEquals("", empty.value.get().toString());

        StringBuilderBean withValue = jsonProcessor.readValue(aposToQuotes("{'value':'a'}"), StringBuilderBean.class);
        assertTrue(withValue.value.isPresent());
        assertEquals("a", withValue.value.get().toString());
    }

    // Regression guard: an enum with no custom deserializer keeps mapping an empty string to
    // undefined(), same as before this change.
    @Test
    void testEnumWithEmptyStillUndefined() throws Exception {
        EnumBean bean = jsonProcessor.readValue(aposToQuotes("{'value':''}"), EnumBean.class);
        assertNotNull(bean.value);
        assertFalse(bean.value.isPresent());
    }

    // Regression guard: a POJO with no custom deserializer (i.e. Jackson's own default
    // BeanDeserializer) keeps mapping an empty string to undefined(), same as before this
    // change, instead of letting BeanDeserializer see the string and fail to construct it.
    @Test
    void testPojoWithoutCustomDeserializerAndEmptyStillUndefined() throws Exception {
        PlainPojoBean bean = jsonProcessor.readValue(aposToQuotes("{'value':''}"), PlainPojoBean.class);
        assertNotNull(bean.value);
        assertFalse(bean.value.isPresent());
    }

    // Interaction (neither side had this): a user-registered deserializer keeps deciding for
    // itself even with mapBlankStringToNull(true), because isStandardDeserializer is false for
    // it (see issue #46) and the guard the option lives in never runs. The option must never
    // override that decision into a present null.
    @Test
    void testCustomDeserializerWinsOverMapBlankStringToNull() throws Exception {
        jsonProcessor.mapperWithModule(true);
        registerCustomDeserializedBeanDeserializer(jsonProcessor);

        JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBeanBox box = jsonProcessor.readValue(
                aposToQuotes("{'value':''}"), JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBeanBox.class);
        assertTrue(box.value.isPresent());
        assertSame(JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBean.EMPTY_SENTINEL, box.value.get());
    }

    // Registers the #46 custom-deserializer fixture from JsonNullWithEmptyCustomDeserializerTest
    // on the given processor's mapper/builder. Reaches into the processor's package-private
    // mapper/builder field (same package) rather than growing JsonProcessor with a
    // generation-specific registration method for a single pair of tests.
    private static void registerCustomDeserializedBeanDeserializer(JsonProcessor jsonProcessor) {
        if (jsonProcessor instanceof Jackson2Processor) {
            com.fasterxml.jackson.databind.module.SimpleModule custom = new com.fasterxml.jackson.databind.module.SimpleModule();
            custom.addDeserializer(JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBean.class,
                    new JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBeanJackson2Deserializer());
            ((Jackson2Processor) jsonProcessor).mapper.registerModule(custom);
            return;
        }
        if (jsonProcessor instanceof Jackson3Processor) {
            tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
            custom.addDeserializer(JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBean.class,
                    new JsonNullWithEmptyCustomDeserializerTest.CustomDeserializedBeanJackson3Deserializer());
            ((Jackson3Processor) jsonProcessor).builder.addModule(custom);
            return;
        }
        throw new RuntimeException("jsonProcessor type not implemented");
    }

    private enum TypeReferences {
        INTEGER {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<Integer>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<Integer>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        },
        STRING {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<String>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<String>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        };

        public abstract Object getType(JsonProcessor jsonProcessor);
    }

}