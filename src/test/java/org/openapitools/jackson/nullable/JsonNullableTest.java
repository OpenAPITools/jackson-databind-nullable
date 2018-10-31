package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class JsonNullableTest extends ModuleTestBase
{
    private static final TypeReference<JsonNullable<String>> JSON_NULLABLE_STRING_TYPE = new TypeReference<JsonNullable<String>>() {};
    private static final TypeReference<JsonNullable<TestBean>> JSON_NULLABLE_BEAN_TYPE = new TypeReference<JsonNullable<TestBean>>() {};

    public static class TestBean
    {
        public int foo;
        public String bar;

        @JsonCreator
        public TestBean(@JsonProperty("foo") int foo, @JsonProperty("bar") String bar)
        {
            this.foo = foo;
            this.bar = bar;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj.getClass() != getClass()) {
                return false;
            }
            TestBean castObj = (TestBean) obj;
            return castObj.foo == foo &&
                    (castObj.bar != null && castObj.bar.equals(bar));
        }

        @Override
        public int hashCode() {
            return foo ^ bar.hashCode();
        }
    }

    static class JsonNullableStringBean {
        public JsonNullable<String> value;

        public JsonNullableStringBean() { }
        JsonNullableStringBean(String str) {
            value = JsonNullable.of(str);
        }
    }

    // [datatype-jdk8#4]
    static class Issue4Entity {
        private JsonNullable<String> data;

        @JsonCreator
        public Issue4Entity(@JsonProperty("data") JsonNullable<String> data) {
            if (data == null)
                throw new NullPointerException("data");
            this.data = data;
        }

        @JsonProperty ("data")
        public JsonNullable<String> data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Issue4Entity entity = (Issue4Entity) o;
            return data.equals(entity.data);
        }
    }

    static class CaseChangingStringWrapper {
        @JsonSerialize(contentUsing=UpperCasingSerializer.class)
        @JsonDeserialize(contentUsing=LowerCasingDeserializer.class)
        public JsonNullable<String> value = JsonNullable.undefined();

        CaseChangingStringWrapper() { }
        public CaseChangingStringWrapper(String s) { value = JsonNullable.of(s); }
    }

    @SuppressWarnings("serial")
    public static class UpperCasingSerializer extends StdScalarSerializer<String>
    {
        public UpperCasingSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(value.toUpperCase());
        }
    }

    @SuppressWarnings("serial")
    public static class LowerCasingDeserializer extends StdScalarDeserializer<String>
    {
        public LowerCasingDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            return p.getText().toLowerCase();
        }
    }

    private ObjectMapper MAPPER;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MAPPER = mapperWithModule();
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testStringAbsent() throws Exception
    {
        assertNull(roundtrip(JsonNullable.<String>undefined(), JSON_NULLABLE_STRING_TYPE).get());
    }
    public void testStringNull() throws Exception
    {
        assertNull(roundtrip(JsonNullable.<String>of(null), JSON_NULLABLE_STRING_TYPE).get());
    }

    public void testStringPresent() throws Exception
    {
        assertEquals("test", roundtrip(JsonNullable.of("test"), JSON_NULLABLE_STRING_TYPE).get());
    }

    public void testBeanAdsent() throws Exception
    {
        assertNull(roundtrip(JsonNullable.<TestBean>undefined(), JSON_NULLABLE_BEAN_TYPE).get());
    }

    public void testBeanNull() throws Exception
    {
        assertNull(roundtrip(JsonNullable.<TestBean>of(null), JSON_NULLABLE_BEAN_TYPE).get());
    }

    public void testBeanPresent() throws Exception
    {
        final TestBean bean = new TestBean(Integer.MAX_VALUE, "woopwoopwoopwoopwoop");
        assertEquals(bean, roundtrip(JsonNullable.of(bean), JSON_NULLABLE_BEAN_TYPE).get());
    }

    // [issue#4]
    public void testBeanWithCreator() throws Exception
    {
        final Issue4Entity emptyEntity = new Issue4Entity(JsonNullable.<String>of(null));
        final String json = MAPPER.writeValueAsString(emptyEntity);

        final Issue4Entity deserialisedEntity = MAPPER.readValue(json, Issue4Entity.class);
        if (!deserialisedEntity.equals(emptyEntity)) {
            throw new IOException("Entities not equal");
        }
    }

    // [issue#4]
    public void testJsonNullableStringInBean() throws Exception
    {
        JsonNullableStringBean bean = MAPPER.readValue("{\"value\":\"xyz\"}", JsonNullableStringBean.class);
        assertNotNull(bean.value);
        assertEquals("xyz", bean.value.get());
    }

    // To support [datatype-jdk8#8]
    public void testExcludeIfJsonNullableAbsent() throws Exception
    {
        ObjectMapper mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':'foo'}"),
                mapper.writeValueAsString(new JsonNullableStringBean("foo")));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new JsonNullableStringBean(null)));

        // however:
        mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':'foo'}"),
                mapper.writeValueAsString(new JsonNullableStringBean("foo")));
        assertEquals(aposToQuotes("{\"value\":null}"),
                mapper.writeValueAsString(new JsonNullableStringBean(null)));
    }

    public void testWithCustomDeserializer() throws Exception
    {
        CaseChangingStringWrapper w = MAPPER.readValue(aposToQuotes("{'value':'FoobaR'}"),
                CaseChangingStringWrapper.class);
        assertEquals("foobar", w.value.get());
    }

    // [modules-java8#36]
    public void testWithCustomDeserializerIfJsonNullableAbsent() throws Exception
    {
        // 10-Aug-2017, tatu: Actually this is not true: missing value does not trigger
        //    specific handling
        /*
        assertEquals(JsonNullable.empty(), MAPPER.readValue("{}",
                CaseChangingStringWrapper.class).value);
                */

        assertEquals(JsonNullable.of(null), MAPPER.readValue(aposToQuotes("{'value':null}"),
                CaseChangingStringWrapper.class).value);
    }

    public void testCustomSerializer() throws Exception
    {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new CaseChangingStringWrapper(VALUE));
        assertEquals(json, aposToQuotes("{'value':'FOOBAR'}"));
    }

    public void testCustomSerializerIfJsonNullableAbsent() throws Exception
    {
        ObjectMapper mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':'FOO'}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper("foo")));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper(null)));
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper()));

        // however:
        mapper = mapperWithModule()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':'FOO'}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper("foo")));
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper(null)));
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper()));
    }

    // [modules-java8#33]: Verify against regression...
    public void testOtherRefSerializers() throws Exception
    {
        String json = MAPPER.writeValueAsString(new AtomicReference<String>("foo"));
        assertEquals(quote("foo"), json);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private <T> JsonNullable<T> roundtrip(JsonNullable<T> obj, TypeReference<JsonNullable<T>> type) throws IOException
    {
        String bytes = MAPPER.writeValueAsString(obj);
        return MAPPER.readValue(bytes, type);
    }
}