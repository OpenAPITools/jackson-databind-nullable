package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass
@MethodSource("jsonProcessors")
class JsonNullableTest extends ModuleTestBase {

    private Object jsonNullableStringType;
    private Object jsonNullableBeanType;


    public static class TestBean {
        public int foo;
        public String bar;

        @JsonCreator
        public TestBean(@JsonProperty("foo") int foo, @JsonProperty("bar") String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @Override
        public boolean equals(Object obj) {
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

        public JsonNullableStringBean() {
        }

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

        @JsonProperty("data")
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

    @Parameter
    JsonProcessor jsonProcessor;

    @BeforeEach
    void setUp() {
        jsonProcessor.mapperWithModule();
        jsonNullableStringType = TypeReferences.STRING.getType(jsonProcessor);
        jsonNullableBeanType = TypeReferences.TEST_BEAN.getType(jsonProcessor);
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    void testStringAbsent() throws Exception {
        assertNull(roundtrip(JsonNullable.<String>undefined(), jsonNullableStringType).get());
    }

    @Test
    void testStringNull() throws Exception {
        assertNull(roundtrip(JsonNullable.<String>of(null), jsonNullableStringType).get());
    }

    @Test
    void testStringPresent() throws Exception {
        assertEquals("test", roundtrip(JsonNullable.of("test"), jsonNullableStringType).get());
    }

    @Test
    void testBeanAdsent() throws Exception {
        assertNull(roundtrip(JsonNullable.<TestBean>undefined(), jsonNullableBeanType).get());
    }

    @Test
    void testBeanNull() throws Exception {
        assertNull(roundtrip(JsonNullable.<TestBean>of(null), jsonNullableBeanType).get());
    }

    @Test
    void testBeanPresent() throws Exception {
        final TestBean bean = new TestBean(Integer.MAX_VALUE, "woopwoopwoopwoopwoop");
        assertEquals(bean, roundtrip(JsonNullable.of(bean), jsonNullableBeanType).get());
    }

    // [issue#4]
    @Test
    void testBeanWithCreator() throws Exception {
        final Issue4Entity emptyEntity = new Issue4Entity(JsonNullable.<String>of(null));
        final String json = jsonProcessor.writeValueAsString(emptyEntity);

        final Issue4Entity deserialisedEntity = jsonProcessor.readValue(json, Issue4Entity.class);
        if (!deserialisedEntity.equals(emptyEntity)) {
            throw new IOException("Entities not equal");
        }
    }

    // [issue#4]
    @Test
    void testJsonNullableStringInBean() throws Exception {
        JsonNullableStringBean bean = jsonProcessor.readValue("{\"value\":\"xyz\"}", JsonNullableStringBean.class);
        assertNotNull(bean.value);
        assertEquals("xyz", bean.value.get());
    }

    // To support [datatype-jdk8#8]
    @Test
    void testExcludeIfJsonNullableAbsent() throws Exception {
        jsonProcessor
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':'foo'}"),
                jsonProcessor.writeValueAsString(new JsonNullableStringBean("foo")));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                jsonProcessor.writeValueAsString(new JsonNullableStringBean(null)));

        // however:
        jsonProcessor.mapperWithModule()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':'foo'}"),
                jsonProcessor.writeValueAsString(new JsonNullableStringBean("foo")));
        assertEquals(aposToQuotes("{\"value\":null}"),
                jsonProcessor.writeValueAsString(new JsonNullableStringBean(null)));
    }

    @Test
    void testWithCustomDeserializer() throws Exception {
        JsonProcessor.CaseChangingStringWrapper w = jsonProcessor.readValue(aposToQuotes("{'value':'FoobaR'}"),
                jsonProcessor.getCaseChangingStringWrapperClass());
        assertEquals("foobar", w.getValue().get());
    }

    // [modules-java8#36]
    @Test
    void testWithCustomDeserializerIfJsonNullableAbsent() throws Exception {
        // 10-Aug-2017, tatu: Actually this is not true: missing value does not trigger
        //    specific handling
        /*
        assertEquals(JsonNullable.empty(), MAPPER.readValue("{}",
                CaseChangingStringWrapper.class).value);
                */

        assertEquals(JsonNullable.of(null), jsonProcessor.readValue(aposToQuotes("{'value':null}"),
                jsonProcessor.getCaseChangingStringWrapperClass()).getValue());
    }

    @Test
    void testCustomSerializer() throws Exception {
        final String VALUE = "fooBAR";
        String json = jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper(VALUE));
        assertEquals(json, aposToQuotes("{'value':'FOOBAR'}"));
    }

    @Test
    void testCustomSerializerIfJsonNullableAbsent() throws Exception {
        jsonProcessor.mapperWithModule()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':'FOO'}"),
                jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper("foo")));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper(null)));
        assertEquals(aposToQuotes("{}"),
                jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper()));

        // however:
        jsonProcessor.mapperWithModule()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':'FOO'}"),
                jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper("foo")));
        assertEquals(aposToQuotes("{'value':null}"),
                jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper(null)));
        assertEquals(aposToQuotes("{}"),
                jsonProcessor.writeValueAsString(jsonProcessor.getCaseChangingStringWrapper()));
    }

    // [modules-java8#33]: Verify against regression...
    @Test
    void testOtherRefSerializers() throws Exception {
        String json = jsonProcessor.writeValueAsString(new AtomicReference<String>("foo"));
        assertEquals(quote("foo"), json);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private <T> JsonNullable<T> roundtrip(JsonNullable<T> obj, Object type) throws Exception {
        String bytes = jsonProcessor.writeValueAsString(obj);
        return jsonProcessor.readValue(bytes, type);
    }

    private enum TypeReferences {
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
        },
        TEST_BEAN {
            @Override
            public Object getType(JsonProcessor jsonProcessor) {
                if (jsonProcessor instanceof Jackson2Processor) {
                    return new TypeReference<JsonNullable<TestBean>>() {
                    };
                }
                if (jsonProcessor instanceof Jackson3Processor) {
                    return new tools.jackson.core.type.TypeReference<JsonNullable<TestBean>>() {
                    };
                }
                throw new RuntimeException("jsonProcessor type not implemented");
            }
        };

        public abstract Object getType(JsonProcessor jsonProcessor);
    }
}
