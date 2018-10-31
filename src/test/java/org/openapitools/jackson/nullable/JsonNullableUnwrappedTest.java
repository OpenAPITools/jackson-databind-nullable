package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import java.util.concurrent.atomic.AtomicReference;

public class JsonNullableUnwrappedTest extends ModuleTestBase
{
    static class Child {
        public String name = "Bob";
    }

    static class Parent {
        private Child child = new Child();

        @JsonUnwrapped
        public Child getChild() {
            return child;
        }
    }

    static class JsonNullableParent {
        @JsonUnwrapped(prefix = "XX.")
        public JsonNullable<Child> child = JsonNullable.of(new Child());
    }

    static class Bean {
        public String id;
        @JsonUnwrapped(prefix="child")
        public JsonNullable<Bean2> bean2;

        public Bean(String id, JsonNullable<Bean2> bean2) {
            this.id = id;
            this.bean2 = bean2;
        }
    }

    static class Bean2 {
        public String name;
    }

    public void testUntypedWithJsonNullablesNotNulls() throws Exception
    {
        final ObjectMapper mapper = mapperWithModule();
        String jsonExp = aposToQuotes("{'XX.name':'Bob'}");
        String jsonAct = mapper.writeValueAsString(new JsonNullableParent());
        assertEquals(jsonExp, jsonAct);
    }

    // for [datatype-jdk8#20]
    public void testShouldSerializeUnwrappedJsonNullable() throws Exception {
        final ObjectMapper mapper = mapperWithModule();

        assertEquals("{\"id\":\"foo\"}",
                mapper.writeValueAsString(new Bean("foo", JsonNullable.<Bean2>undefined())));
    }

    // for [datatype-jdk8#26]
    public void testPropogatePrefixToSchema() throws Exception {
        final ObjectMapper mapper = mapperWithModule();

        final AtomicReference<String> propertyName = new AtomicReference<String>();
        mapper.acceptJsonFormatVisitor(JsonNullableParent.class, new JsonFormatVisitorWrapper.Base(new DefaultSerializerProvider.Impl()) {
            @Override
            public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
                return new JsonObjectFormatVisitor.Base(getProvider()) {
                    @Override
                    public void optionalProperty(BeanProperty prop) {
                        propertyName.set(prop.getName());
                    }
                };
            }
        });

        assertEquals("XX.name", propertyName.get());
    }
}