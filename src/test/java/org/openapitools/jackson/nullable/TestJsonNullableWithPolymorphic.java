package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ParameterizedClass
@MethodSource("jsonProcessors")
class TestJsonNullableWithPolymorphic extends ModuleTestBase {
    static class ContainerA {
        @JsonProperty
        private JsonNullable<String> name = JsonNullable.undefined();
        @JsonProperty
        private JsonNullable<Strategy> strategy = JsonNullable.undefined();
    }

    static class ContainerB {
        @JsonProperty
        private JsonNullable<String> name = JsonNullable.undefined();
        @JsonProperty
        private Strategy strategy = null;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(name = "Foo", value = Foo.class),
            @JsonSubTypes.Type(name = "Bar", value = Bar.class),
            @JsonSubTypes.Type(name = "Baz", value = Baz.class)})
    interface Strategy {
    }

    static class Foo implements Strategy {
        @JsonProperty
        private final int foo;

        @JsonCreator
        Foo(@JsonProperty("foo") int foo) {
            this.foo = foo;
        }
    }

    static class Bar implements Strategy {
        @JsonProperty
        private final boolean bar;

        @JsonCreator
        Bar(@JsonProperty("bar") boolean bar) {
            this.bar = bar;
        }
    }

    static class Baz implements Strategy {
        @JsonProperty
        private final String baz;

        @JsonCreator
        Baz(@JsonProperty("baz") String baz) {
            this.baz = baz;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Parameter
    JsonProcessor jsonProcessor;

    @BeforeEach
    void setup() {
        jsonProcessor.mapperWithModule();
    }

    @Test
    void testJsonNullableMapsFoo() throws Exception {

        Map<String, Object> foo = new LinkedHashMap<String, Object>();
        Map<String, Object> loop = new LinkedHashMap<String, Object>();
        loop.put("type", "Foo");
        loop.put("foo", 42);

        foo.put("name", "foo strategy");
        foo.put("strategy", loop);

        _test(jsonProcessor, foo);
    }

    @Test
    void testJsonNullableMapsBar() throws Exception {

        Map<String, Object> bar = new LinkedHashMap<String, Object>();
        Map<String, Object> loop = new LinkedHashMap<String, Object>();
        loop.put("type", "Bar");
        loop.put("bar", true);

        bar.put("name", "bar strategy");
        bar.put("strategy", loop);

        _test(jsonProcessor, bar);
    }

    @Test
    void testJsonNullableMapsBaz() throws Exception {
        Map<String, Object> baz = new LinkedHashMap<String, Object>();
        Map<String, Object> loop = new LinkedHashMap<String, Object>();
        loop.put("type", "Baz");
        loop.put("baz", "hello world!");

        baz.put("name", "bar strategy");
        baz.put("strategy", loop);

        _test(jsonProcessor, baz);
    }

    @Test
    void testJsonNullableWithTypeAnnotation13() throws Exception {
        JsonProcessor.AbstractJsonNullable result = jsonProcessor.readValue("{\"value\" : 5}",
                jsonProcessor.getAbstractJsonNullableClass());
        assertNotNull(result);
        assertNotNull(result.getValue());
        Object ob = result.getValue().get();
        assertEquals(Integer.class, ob.getClass());
        assertEquals(Integer.valueOf(5), ob);
    }

    private void _test(JsonProcessor m, Map<String, ?> map) throws Exception {
        String json = m.writeValueAsString(map);

        ContainerA objA = m.readValue(json, ContainerA.class);
        assertNotNull(objA);

        ContainerB objB = m.readValue(json, ContainerB.class);
        assertNotNull(objB);
    }
}
