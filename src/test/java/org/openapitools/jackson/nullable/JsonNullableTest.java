package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonNullableTest extends ModuleTestBase
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

    public static class UpperCasingSerializer extends StdScalarSerializer<String>
    {
        public UpperCasingSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                              SerializationContext provider) throws JacksonException {
            gen.writeString(value.toUpperCase());
        }
    }

    public static class LowerCasingDeserializer extends StdScalarDeserializer<String>
    {
        public LowerCasingDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt)
                throws JacksonException {
            return p.getString().toLowerCase();
        }
    }

    private ObjectMapper MAPPER;

    @BeforeAll
    void setUp() {
        MAPPER = mapperWithModule();
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    void testStringAbsent() {
        assertNull(roundtrip(JsonNullable.<String>undefined(), JSON_NULLABLE_STRING_TYPE).get());
    }
    @Test
    void testStringNull() {
        assertNull(roundtrip(JsonNullable.<String>of(null), JSON_NULLABLE_STRING_TYPE).get());
    }

    @Test
    void testStringPresent() {
        assertEquals("test", roundtrip(JsonNullable.of("test"), JSON_NULLABLE_STRING_TYPE).get());
    }

    @Test
    void testBeanAdsent() {
        assertNull(roundtrip(JsonNullable.<TestBean>undefined(), JSON_NULLABLE_BEAN_TYPE).get());
    }

    @Test
    void testBeanNull() {
        assertNull(roundtrip(JsonNullable.<TestBean>of(null), JSON_NULLABLE_BEAN_TYPE).get());
    }

    @Test
    void testBeanPresent() {
        final TestBean bean = new TestBean(Integer.MAX_VALUE, "woopwoopwoopwoopwoop");
        assertEquals(bean, roundtrip(JsonNullable.of(bean), JSON_NULLABLE_BEAN_TYPE).get());
    }

    // [issue#4]
    @Test
    void testBeanWithCreator() {
        final Issue4Entity emptyEntity = new Issue4Entity(JsonNullable.<String>of(null));
        final String json = MAPPER.writeValueAsString(emptyEntity);

        final Issue4Entity deserialisedEntity = MAPPER.readValue(json, Issue4Entity.class);
        assertEquals(emptyEntity, deserialisedEntity);
    }

    // [issue#4]
    @Test
    void testJsonNullableStringInBean() {
        JsonNullableStringBean bean = MAPPER.readValue("{\"value\":\"xyz\"}", JsonNullableStringBean.class);
        assertNotNull(bean.value);
        assertEquals("xyz", bean.value.get());
    }

    // To support [datatype-jdk8#8]
    @Test
    void testExcludeIfJsonNullableAbsent() {
        ObjectMapper mapper = mapperWithModule(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':'foo'}"),
                mapper.writeValueAsString(new JsonNullableStringBean("foo")));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new JsonNullableStringBean(null)));

        // however:
        mapper = mapperWithModule(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':'foo'}"),
                mapper.writeValueAsString(new JsonNullableStringBean("foo")));
        assertEquals(aposToQuotes("{\"value\":null}"),
                mapper.writeValueAsString(new JsonNullableStringBean(null)));
    }

    @Test
    void testWithCustomDeserializer() {
        CaseChangingStringWrapper w = MAPPER.readValue(aposToQuotes("{'value':'FoobaR'}"),
                CaseChangingStringWrapper.class);
        assertEquals("foobar", w.value.get());
    }

    // [modules-java8#36]
    @Test
    void testWithCustomDeserializerIfJsonNullableAbsent() {
        // 10-Aug-2017, tatu: Actually this is not true: missing value does not trigger
        //    specific handling
        /*
        assertEquals(JsonNullable.empty(), MAPPER.readValue("{}",
                CaseChangingStringWrapper.class).value);
                */

        assertEquals(JsonNullable.of(null), MAPPER.readValue(aposToQuotes("{'value':null}"),
                CaseChangingStringWrapper.class).value);
    }

    @Test
    void testCustomSerializer() {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new CaseChangingStringWrapper(VALUE));
        assertEquals(json, aposToQuotes("{'value':'FOOBAR'}"));
    }

    @Test
    void testCustomSerializerIfJsonNullableAbsent() {
        ObjectMapper mapper = mapperWithModule(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':'FOO'}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper("foo")));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper(null)));
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper()));

        // however:
        mapper = mapperWithModule(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':'FOO'}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper("foo")));
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper(null)));
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper()));
    }

    // [modules-java8#33]: Verify against regression...
    @Test
    void testOtherRefSerializers() {
        String json = MAPPER.writeValueAsString(new AtomicReference<String>("foo"));
        assertEquals(quote("foo"), json);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private <T> JsonNullable<T> roundtrip(JsonNullable<T> obj, TypeReference<JsonNullable<T>> type) {
        String bytes = MAPPER.writeValueAsString(obj);
        return MAPPER.readValue(bytes, type);
    }
}