package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.EnumResolver;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        // ValueDeserializer.handledType() defaults to null; DelegatingDeserializer's
        // constructor requires the delegatee's handledType() to be non-null (it feeds
        // StdDeserializer's own required-handled-type check), which only matters once this
        // deserializer is wrapped, as in testJackson3DelegatingWrapperAroundApplicationDeserializerStillSeesEmptyString.
        @Override
        public Class<CustomDeserializedBean> handledType() {
            return CustomDeserializedBean.class;
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

    // --------------------------------------------------------------------------------------
    // Regression coverage for the innermost-delegate classification (review of #187 by
    // Thorsrud22): an application registers a BeanDeserializerModifier that wraps the
    // resolved deserializer for a type in a DelegatingDeserializer subclass of its own -
    // e.g. a logging or validation wrapper - which forwards everything to what it wraps and
    // decides nothing about blank strings itself. Before unwrapping, this library classified
    // the wrapper by its own (application) class name, which made JsonNullable<Pojo> given ""
    // throw instead of returning undefined() as it does without the wrapper. Classifying by
    // the innermost deserializer restores that.
    // --------------------------------------------------------------------------------------

    // A no-op forwarding wrapper: it does not special-case "" itself, so it stands in for
    // any application decorator (logging, validation, metrics) that a BeanDeserializerModifier
    // installs around a type's real deserializer.
    static class PassThroughJackson2Deserializer extends DelegatingDeserializer {
        PassThroughJackson2Deserializer(JsonDeserializer<?> delegate) {
            super(delegate);
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
            return new PassThroughJackson2Deserializer(newDelegatee);
        }
    }

    static class PassThroughJackson3Deserializer extends tools.jackson.databind.deser.std.DelegatingDeserializer {
        PassThroughJackson3Deserializer(tools.jackson.databind.ValueDeserializer<?> delegate) {
            super(delegate);
        }

        @Override
        protected tools.jackson.databind.ValueDeserializer<?> newDelegatingInstance(tools.jackson.databind.ValueDeserializer<?> newDelegatee) {
            return new PassThroughJackson3Deserializer(newDelegatee);
        }
    }

    // Wraps whatever deserializer was resolved for targetClass in a pass-through delegating
    // wrapper, whether that deserializer is Jackson's own BeanDeserializer (nothing else
    // registered for the type) or an application-registered one (addDeserializer ran first;
    // BeanDeserializerModifier.modifyDeserializer still gets a look at the result, see
    // BeanDeserializerFactory.createBeanDeserializer, "[databind#2392]").
    static class DelegatingWrapJackson2Modifier extends BeanDeserializerModifier {
        private final Class<?> targetClass;

        DelegatingWrapJackson2Modifier(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                                                       JsonDeserializer<?> deserializer) {
            if (beanDesc.getBeanClass() == targetClass) {
                return new PassThroughJackson2Deserializer(deserializer);
            }
            return deserializer;
        }
    }

    static class DelegatingWrapJackson3Modifier extends tools.jackson.databind.deser.ValueDeserializerModifier {
        private final Class<?> targetClass;

        DelegatingWrapJackson3Modifier(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public tools.jackson.databind.ValueDeserializer<?> modifyDeserializer(
                tools.jackson.databind.DeserializationConfig config,
                tools.jackson.databind.BeanDescription.Supplier beanDesc,
                tools.jackson.databind.ValueDeserializer<?> deserializer) {
            if (beanDesc.getBeanClass() == targetClass) {
                return new PassThroughJackson3Deserializer(deserializer);
            }
            return deserializer;
        }
    }

    // Plain POJO with no custom deserializer of its own, so the wrapper installed by
    // DelegatingWrapJackson{2,3}Modifier wraps Jackson's own BeanDeserializer - the shape
    // Thorsrud22 measured the regression with.
    static class WrappedPojo {
        public String a;
    }

    static class WrappedPojoBox {
        public JsonNullable<WrappedPojo> value;
    }

    // (a) Regression case: an application BeanDeserializerModifier wraps Jackson's own
    // BeanDeserializer in a DelegatingDeserializer subclass. Before unwrapping, the wrapper's
    // own (application) class name made this non-standard and threw InvalidFormatException
    // instead of returning undefined() - see the pre-change run recorded in the task report.
    @Test
    void testJackson2DelegatingWrapperAroundBeanDeserializerYieldsUndefinedForEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        SimpleModule custom = new SimpleModule();
        custom.setDeserializerModifier(new DelegatingWrapJackson2Modifier(WrappedPojo.class));
        mapper.registerModule(custom);

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson2DelegatingWrapperAroundBeanDeserializerYieldsPresentNullWithMapBlankStringToNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule().mapBlankStringToNull(true));
        SimpleModule custom = new SimpleModule();
        custom.setDeserializerModifier(new DelegatingWrapJackson2Modifier(WrappedPojo.class));
        mapper.registerModule(custom);

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertTrue(box.value.isPresent());
        assertNull(box.value.get());
    }

    // Jackson 3 twins of the two tests above.
    @Test
    void testJackson3DelegatingWrapperAroundBeanDeserializerYieldsUndefinedForEmptyString() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.setDeserializerModifier(new DelegatingWrapJackson3Modifier(WrappedPojo.class));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(custom)
                .build();

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson3DelegatingWrapperAroundBeanDeserializerYieldsPresentNullWithMapBlankStringToNull() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.setDeserializerModifier(new DelegatingWrapJackson3Modifier(WrappedPojo.class));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module().mapBlankStringToNull(true))
                .addModule(custom)
                .build();

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertTrue(box.value.isPresent());
        assertNull(box.value.get());
    }

    // (b) A DelegatingDeserializer subclass wrapping an APPLICATION-owned deserializer: the
    // innermost deserializer is still application-owned, so the wrapped type keeps deciding
    // for itself, exactly as testJackson{2,3}CustomDeserializerReceivesEmptyString above -
    // this pins that unwrapping classifies by the innermost delegate rather than, say,
    // treating every DelegatingDeserializer subclass as standard.
    @Test
    void testJackson2DelegatingWrapperAroundApplicationDeserializerStillSeesEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        SimpleModule custom = new SimpleModule();
        custom.addDeserializer(CustomDeserializedBean.class, new CustomDeserializedBeanJackson2Deserializer());
        custom.setDeserializerModifier(new DelegatingWrapJackson2Modifier(CustomDeserializedBean.class));
        mapper.registerModule(custom);

        CustomDeserializedBeanBox box = mapper.readValue(aposToQuotes("{'value':''}"), CustomDeserializedBeanBox.class);
        assertTrue(box.value.isPresent());
        assertSame(CustomDeserializedBean.EMPTY_SENTINEL, box.value.get());
    }

    @Test
    void testJackson3DelegatingWrapperAroundApplicationDeserializerStillSeesEmptyString() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.addDeserializer(CustomDeserializedBean.class, new CustomDeserializedBeanJackson3Deserializer());
        custom.setDeserializerModifier(new DelegatingWrapJackson3Modifier(CustomDeserializedBean.class));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(custom)
                .build();

        CustomDeserializedBeanBox box = mapper.readValue(aposToQuotes("{'value':''}"), CustomDeserializedBeanBox.class);
        assertTrue(box.value.isPresent());
        assertSame(CustomDeserializedBean.EMPTY_SENTINEL, box.value.get());
    }

    // --------------------------------------------------------------------------------------
    // (c) @JsonDeserialize(contentConverter = ...) on a JsonNullable<Pojo> property: the
    // converter resolves through Jackson's own StdDelegatingDeserializer (Jackson 2) /
    // StdConvertingDeserializer (Jackson 3, renamed) as the content deserializer, which is
    // already classified as Jackson's own without any unwrapping (its class lives in the
    // Jackson namespace). Documents and pins the behaviour Thorsrud22 flagged as unchanged
    // from master: a blank string still takes the undefined()/mapBlankStringToNull shortcut
    // and the converter is never invoked for it, but runs for a non-blank string.
    //
    // Property-level @JsonDeserialize(converter = ...) (as opposed to contentConverter) is
    // deliberately not exercised here: per its own javadoc it converts into the ACTUAL
    // property type, i.e. JsonNullable<Pojo> itself, which replaces this library's
    // deserializer for the property outright rather than becoming the delegate it wraps -
    // there would be nothing of this library's logic left to test.
    // --------------------------------------------------------------------------------------

    static class ConvertedPojo {
        public String tag;

        public ConvertedPojo() {
        }

        public ConvertedPojo(String tag) {
            this.tag = tag;
        }
    }

    static final AtomicInteger JACKSON2_CONTENT_CONVERTER_INVOCATIONS = new AtomicInteger();
    static final AtomicInteger JACKSON3_CONTENT_CONVERTER_INVOCATIONS = new AtomicInteger();

    static class Jackson2StringToConvertedPojoConverter extends StdConverter<String, ConvertedPojo> {
        @Override
        public ConvertedPojo convert(String value) {
            JACKSON2_CONTENT_CONVERTER_INVOCATIONS.incrementAndGet();
            return new ConvertedPojo(value);
        }
    }

    static class Jackson3StringToConvertedPojoConverter extends tools.jackson.databind.util.StdConverter<String, ConvertedPojo> {
        @Override
        public ConvertedPojo convert(String value) {
            JACKSON3_CONTENT_CONVERTER_INVOCATIONS.incrementAndGet();
            return new ConvertedPojo(value);
        }
    }

    static class Jackson2ContentConverterBox {
        @JsonDeserialize(contentConverter = Jackson2StringToConvertedPojoConverter.class)
        public JsonNullable<ConvertedPojo> value;
    }

    static class Jackson3ContentConverterBox {
        @tools.jackson.databind.annotation.JsonDeserialize(contentConverter = Jackson3StringToConvertedPojoConverter.class)
        public JsonNullable<ConvertedPojo> value;
    }

    @Test
    void testJackson2ContentConverterSkippedForEmptyStringButInvokedOtherwise() throws Exception {
        JACKSON2_CONTENT_CONVERTER_INVOCATIONS.set(0);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        Jackson2ContentConverterBox empty = mapper.readValue(aposToQuotes("{'value':''}"), Jackson2ContentConverterBox.class);
        assertNotNull(empty.value);
        assertFalse(empty.value.isPresent());
        assertEquals(0, JACKSON2_CONTENT_CONVERTER_INVOCATIONS.get());

        Jackson2ContentConverterBox withValue = mapper.readValue(aposToQuotes("{'value':'hi'}"), Jackson2ContentConverterBox.class);
        assertTrue(withValue.value.isPresent());
        assertEquals("hi", withValue.value.get().tag);
        assertEquals(1, JACKSON2_CONTENT_CONVERTER_INVOCATIONS.get());
    }

    @Test
    void testJackson3ContentConverterSkippedForEmptyStringButInvokedOtherwise() throws Exception {
        JACKSON3_CONTENT_CONVERTER_INVOCATIONS.set(0);
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .build();

        Jackson3ContentConverterBox empty = mapper.readValue(aposToQuotes("{'value':''}"), Jackson3ContentConverterBox.class);
        assertNotNull(empty.value);
        assertFalse(empty.value.isPresent());
        assertEquals(0, JACKSON3_CONTENT_CONVERTER_INVOCATIONS.get());

        Jackson3ContentConverterBox withValue = mapper.readValue(aposToQuotes("{'value':'hi'}"), Jackson3ContentConverterBox.class);
        assertTrue(withValue.value.isPresent());
        assertEquals("hi", withValue.value.get().tag);
        assertEquals(1, JACKSON3_CONTENT_CONVERTER_INVOCATIONS.get());
    }


    // --------------------------------------------------------------------------------------
    // Regression coverage for the abstract/concrete distinction added to
    // isJacksonOwnDeserializer (found by an independent case sweep after the review of
    // #187 that prompted the innermost-delegate change above): a subclass of one of
    // Jackson's own CONCRETE deserializers (EnumDeserializer, BeanDeserializer,
    // NumberDeserializers.NumberDeserializer) that does not override deserialize() adds
    // nothing to Jackson's own blank-string handling and must keep the shortcut, exactly
    // like using that concrete deserializer unwrapped. Classifying every non-Jackson class
    // name as application-owned (the innermost-delegate change alone) misclassified this
    // case as application-owned instead, since the SUBCLASS's own name is not under
    // Jackson's namespace - only the ancestor it does nothing but inherit from is.
    // --------------------------------------------------------------------------------------

    enum WrappedEnum { ONE, TWO }

    static class WrappedEnumBox {
        public JsonNullable<WrappedEnum> value;
    }

    static class WrappedNumberBox {
        public JsonNullable<Number> value;
    }

    static class NestedJsonNullableBox {
        public JsonNullable<JsonNullable<String>> value;
    }

    // (a) An application subclass of Jackson's own concrete EnumDeserializer, changing
    // nothing about deserialize(), registered for WrappedEnum via addDeserializer.
    static class AppEnumJackson2Deserializer extends EnumDeserializer {
        AppEnumJackson2Deserializer(EnumResolver resolver) {
            super(resolver, false, null, null);
        }
    }

    static class AppEnumJackson3Deserializer extends tools.jackson.databind.deser.jdk.EnumDeserializer {
        AppEnumJackson3Deserializer(tools.jackson.databind.util.EnumResolver resolver) {
            super(resolver, false, null, null);
        }
    }

    // Builds the EnumResolver from public API (AnnotatedClassResolver + EnumResolver's own
    // factory) rather than reaching into Jackson's private construction path - this is
    // exactly the shape of resolver Jackson itself would have built for WrappedEnum.
    private static AppEnumJackson2Deserializer buildAppEnumJackson2Deserializer() {
        DeserializationConfig config = new ObjectMapper().getDeserializationConfig();
        AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, WrappedEnum.class);
        EnumResolver resolver = EnumResolver.constructUsingToString(config, ac);
        return new AppEnumJackson2Deserializer(resolver);
    }

    private static AppEnumJackson3Deserializer buildAppEnumJackson3Deserializer() {
        tools.jackson.databind.DeserializationConfig config =
                tools.jackson.databind.json.JsonMapper.builder().build().deserializationConfig();
        tools.jackson.databind.introspect.AnnotatedClass ac =
                tools.jackson.databind.introspect.AnnotatedClassResolver.resolveWithoutSuperTypes(config, WrappedEnum.class);
        tools.jackson.databind.util.EnumResolver resolver =
                tools.jackson.databind.util.EnumResolver.constructUsingToString(config, ac);
        return new AppEnumJackson3Deserializer(resolver);
    }

    @Test
    void testJackson2EnumDeserializerSubclassYieldsUndefinedForEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        SimpleModule custom = new SimpleModule();
        custom.addDeserializer(WrappedEnum.class, JsonNullWithEmptyCustomDeserializerTest.<WrappedEnum>uncheckedAsJackson2Deserializer(buildAppEnumJackson2Deserializer()));
        mapper.registerModule(custom);

        WrappedEnumBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedEnumBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson2EnumDeserializerSubclassYieldsPresentNullWithMapBlankStringToNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule().mapBlankStringToNull(true));
        SimpleModule custom = new SimpleModule();
        custom.addDeserializer(WrappedEnum.class, JsonNullWithEmptyCustomDeserializerTest.<WrappedEnum>uncheckedAsJackson2Deserializer(buildAppEnumJackson2Deserializer()));
        mapper.registerModule(custom);

        WrappedEnumBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedEnumBox.class);
        assertNotNull(box.value);
        assertTrue(box.value.isPresent());
        assertNull(box.value.get());
    }

    @Test
    void testJackson3EnumDeserializerSubclassYieldsUndefinedForEmptyString() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.addDeserializer(WrappedEnum.class, JsonNullWithEmptyCustomDeserializerTest.<WrappedEnum>uncheckedAsJackson3Deserializer(buildAppEnumJackson3Deserializer()));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(custom)
                .build();

        WrappedEnumBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedEnumBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson3EnumDeserializerSubclassYieldsPresentNullWithMapBlankStringToNull() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.addDeserializer(WrappedEnum.class, JsonNullWithEmptyCustomDeserializerTest.<WrappedEnum>uncheckedAsJackson3Deserializer(buildAppEnumJackson3Deserializer()));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module().mapBlankStringToNull(true))
                .addModule(custom)
                .build();

        WrappedEnumBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedEnumBox.class);
        assertNotNull(box.value);
        assertTrue(box.value.isPresent());
        assertNull(box.value.get());
    }

    // (b) An application subclass of Jackson's own concrete BeanDeserializer, installed by
    // a modifier via the copy constructor both generations expose for exactly this purpose,
    // changing nothing about deserialize().
    static class AppBeanJackson2Deserializer extends BeanDeserializer {
        AppBeanJackson2Deserializer(BeanDeserializerBase src) {
            super(src);
        }
    }

    static class AppBeanJackson3Deserializer extends tools.jackson.databind.deser.bean.BeanDeserializer {
        AppBeanJackson3Deserializer(tools.jackson.databind.deser.bean.BeanDeserializer src) {
            super(src);
        }
    }

    static class BeanSubclassJackson2Modifier extends BeanDeserializerModifier {
        private final Class<?> targetClass;

        BeanSubclassJackson2Modifier(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                                                       JsonDeserializer<?> deserializer) {
            if (beanDesc.getBeanClass() == targetClass && deserializer instanceof BeanDeserializerBase) {
                return new AppBeanJackson2Deserializer((BeanDeserializerBase) deserializer);
            }
            return deserializer;
        }
    }

    static class BeanSubclassJackson3Modifier extends tools.jackson.databind.deser.ValueDeserializerModifier {
        private final Class<?> targetClass;

        BeanSubclassJackson3Modifier(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public tools.jackson.databind.ValueDeserializer<?> modifyDeserializer(
                tools.jackson.databind.DeserializationConfig config,
                tools.jackson.databind.BeanDescription.Supplier beanDesc,
                tools.jackson.databind.ValueDeserializer<?> deserializer) {
            if (beanDesc.getBeanClass() == targetClass && deserializer instanceof tools.jackson.databind.deser.bean.BeanDeserializer) {
                return new AppBeanJackson3Deserializer((tools.jackson.databind.deser.bean.BeanDeserializer) deserializer);
            }
            return deserializer;
        }
    }

    @Test
    void testJackson2BeanDeserializerSubclassYieldsUndefinedForEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        SimpleModule custom = new SimpleModule();
        custom.setDeserializerModifier(new BeanSubclassJackson2Modifier(WrappedPojo.class));
        mapper.registerModule(custom);

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson2BeanDeserializerSubclassYieldsPresentNullWithMapBlankStringToNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule().mapBlankStringToNull(true));
        SimpleModule custom = new SimpleModule();
        custom.setDeserializerModifier(new BeanSubclassJackson2Modifier(WrappedPojo.class));
        mapper.registerModule(custom);

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertTrue(box.value.isPresent());
        assertNull(box.value.get());
    }

    @Test
    void testJackson3BeanDeserializerSubclassYieldsUndefinedForEmptyString() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.setDeserializerModifier(new BeanSubclassJackson3Modifier(WrappedPojo.class));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(custom)
                .build();

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson3BeanDeserializerSubclassYieldsPresentNullWithMapBlankStringToNull() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.setDeserializerModifier(new BeanSubclassJackson3Modifier(WrappedPojo.class));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module().mapBlankStringToNull(true))
                .addModule(custom)
                .build();

        WrappedPojoBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedPojoBox.class);
        assertNotNull(box.value);
        assertTrue(box.value.isPresent());
        assertNull(box.value.get());
    }

    // (c) An application subclass of Jackson's own concrete NumberDeserializers.NumberDeserializer
    // for a JsonNullable<Number>. Its declared type is JsonDeserializer<Object> (the base
    // class's own type parameter), which does not satisfy addDeserializer's
    // JsonDeserializer<? extends T> bound for T=Number - an unchecked cast is unavoidable
    // and safe here since Jackson only ever consults the deserializer's runtime type.
    static class AppNumberJackson2Deserializer extends NumberDeserializers.NumberDeserializer {
    }

    static class AppNumberJackson3Deserializer extends tools.jackson.databind.deser.jdk.NumberDeserializers.NumberDeserializer {
    }

    // EnumDeserializer and NumberDeserializers.NumberDeserializer both declare themselves
    // as *Deserializer<Object> (their own base class's type parameter), which never
    // satisfies addDeserializer's JsonDeserializer<? extends T> bound for a narrower T
    // (WrappedEnum, Number). An unchecked cast is unavoidable and safe here since Jackson
    // only ever consults the deserializer's runtime type, never this static one.
    @SuppressWarnings("unchecked")
    private static <T> JsonDeserializer<T> uncheckedAsJackson2Deserializer(JsonDeserializer<?> raw) {
        return (JsonDeserializer<T>) raw;
    }

    @SuppressWarnings("unchecked")
    private static <T> tools.jackson.databind.ValueDeserializer<T> uncheckedAsJackson3Deserializer(tools.jackson.databind.ValueDeserializer<?> raw) {
        return (tools.jackson.databind.ValueDeserializer<T>) raw;
    }

    @Test
    void testJackson2NumberDeserializerSubclassYieldsUndefinedForEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        SimpleModule custom = new SimpleModule();
        custom.addDeserializer(Number.class, JsonNullWithEmptyCustomDeserializerTest.<Number>uncheckedAsJackson2Deserializer(new AppNumberJackson2Deserializer()));
        mapper.registerModule(custom);

        WrappedNumberBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedNumberBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    @Test
    void testJackson3NumberDeserializerSubclassYieldsUndefinedForEmptyString() throws Exception {
        tools.jackson.databind.module.SimpleModule custom = new tools.jackson.databind.module.SimpleModule();
        custom.addDeserializer(Number.class, JsonNullWithEmptyCustomDeserializerTest.<Number>uncheckedAsJackson3Deserializer(new AppNumberJackson3Deserializer()));
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(custom)
                .build();

        WrappedNumberBox box = mapper.readValue(aposToQuotes("{'value':''}"), WrappedNumberBox.class);
        assertNotNull(box.value);
        assertFalse(box.value.isPresent());
    }

    // (d) JsonNullable<JsonNullable<String>>: the outer's content deserializer is another
    // instance of this library's own deserializer, which must be classified as standard so
    // the outer keeps its own undefined()/mapBlankStringToNull shortcut for "" instead of
    // ever reaching the inner JsonNullable's isStringLike-exempt handling of "". A non-blank
    // string proves the nesting itself still works end to end.
    @Test
    void testJackson2NestedJsonNullableEmptyStringYieldsUndefined() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        NestedJsonNullableBox empty = mapper.readValue(aposToQuotes("{'value':''}"), NestedJsonNullableBox.class);
        assertNotNull(empty.value);
        assertFalse(empty.value.isPresent());

        NestedJsonNullableBox withValue = mapper.readValue(aposToQuotes("{'value':'x'}"), NestedJsonNullableBox.class);
        assertTrue(withValue.value.isPresent());
        assertTrue(withValue.value.get().isPresent());
        assertEquals("x", withValue.value.get().get());
    }

    @Test
    void testJackson3NestedJsonNullableEmptyStringYieldsUndefined() throws Exception {
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .build();

        NestedJsonNullableBox empty = mapper.readValue(aposToQuotes("{'value':''}"), NestedJsonNullableBox.class);
        assertNotNull(empty.value);
        assertFalse(empty.value.isPresent());

        NestedJsonNullableBox withValue = mapper.readValue(aposToQuotes("{'value':'x'}"), NestedJsonNullableBox.class);
        assertTrue(withValue.value.isPresent());
        assertTrue(withValue.value.get().isPresent());
        assertEquals("x", withValue.value.get().get());
    }
}
