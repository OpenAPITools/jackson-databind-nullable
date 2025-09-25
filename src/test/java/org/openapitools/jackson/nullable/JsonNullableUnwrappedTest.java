package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: Make JsonNulllable work with JsonUnwrapped
@Disabled("JsonNullable currently doesnt work with JsonUnwrapped")
class JsonNullableUnwrappedTest extends ModuleTestBase
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

    @Test
    void testUntypedWithJsonNullablesNotNulls() {
        final ObjectMapper mapper = mapperWithModule();
        String jsonExp = aposToQuotes("{'XX.name':'Bob'}");
        String jsonAct = mapper.writeValueAsString(new JsonNullableParent());
        assertEquals(jsonExp, jsonAct);
    }

    // for [datatype-jdk8#20]
    @Test
    void testShouldSerializeUnwrappedJsonNullable() {
        final ObjectMapper mapper = mapperWithModule();

        assertEquals("{\"id\":\"foo\"}",
                mapper.writeValueAsString(new Bean("foo", JsonNullable.<Bean2>undefined())));
    }

    // for [datatype-jdk8#26]
    // @Test
    // void testPropogatePrefixToSchema() {
    //     final ObjectMapper mapper = mapperWithModule();

    //     final AtomicReference<String> propertyName = new AtomicReference<String>();
    //     mapper.acceptJsonFormatVisitor(JsonNullableParent.class, new JsonFormatVisitorWrapper.Base(new DefaultSerializerProvider.Impl()) {
    //         @Override
    //         public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
    //             return new JsonObjectFormatVisitor.Base(getProvider()) {
    //                 @Override
    //                 public void optionalProperty(BeanProperty prop) {
    //                     propertyName.set(prop.getName());
    //                 }
    //             };
    //         }
    //     });

    //     assertEquals("XX.name", propertyName.get());
    // }
}
