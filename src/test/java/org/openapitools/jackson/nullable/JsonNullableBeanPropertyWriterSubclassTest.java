package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.JacksonModule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source- and binary-compatibility guard for the protected constructor chain of
 * {@link JsonNullableJackson2BeanPropertyWriter} and
 * {@link JsonNullableJackson3BeanPropertyWriter}.
 *
 * <p>Libraries subclass these writers and call the protected two-argument
 * {@code (BeanPropertyWriter, PropertyName)} super constructors, mirroring the
 * constructor chain of Jackson's own {@code BeanPropertyWriter}. The subclasses
 * below only compile while those constructors exist (source compatibility), and
 * the reflection checks fail if they are ever removed from the compiled class
 * (binary compatibility). The end-to-end assertions pin the delegation default:
 * the two-argument constructors delegate with {@code allowSuppressNulls = false},
 * so a subclass built on them keeps the pre-{@code allowSuppressNulls} output.
 */
class JsonNullableBeanPropertyWriterSubclassTest extends ModuleTestBase {

    static class Pet {
        public JsonNullable<String> name = JsonNullable.undefined();
        public JsonNullable<Integer> age = JsonNullable.undefined();

        public Pet name(JsonNullable<String> name) {
            this.name = name;
            return this;
        }

        public Pet age(JsonNullable<Integer> age) {
            this.age = age;
            return this;
        }
    }

    // Written against the pre-allowSuppressNulls constructor chain: the two-argument
    // super call compiles only while the protected (BeanPropertyWriter, PropertyName)
    // constructor exists.
    static class Jackson2Subclass extends JsonNullableJackson2BeanPropertyWriter {
        Jackson2Subclass(BeanPropertyWriter base) {
            super(base);
        }

        Jackson2Subclass(BeanPropertyWriter base, PropertyName newName) {
            super(base, newName);
        }

        @Override
        protected BeanPropertyWriter _new(PropertyName newName) {
            return new Jackson2Subclass(this, newName);
        }
    }

    static class Jackson3Subclass extends JsonNullableJackson3BeanPropertyWriter {
        Jackson3Subclass(tools.jackson.databind.ser.BeanPropertyWriter base) {
            super(base);
        }

        Jackson3Subclass(tools.jackson.databind.ser.BeanPropertyWriter base,
                         tools.jackson.databind.PropertyName newName) {
            super(base, newName);
        }

        @Override
        protected tools.jackson.databind.ser.BeanPropertyWriter _new(tools.jackson.databind.PropertyName newName) {
            return new Jackson3Subclass(this, newName);
        }
    }

    @Test
    void testJackson2SubclassKeepsTwoArgConstructorChain() throws Exception {
        // binary compatibility: the protected two-argument constructor still exists
        Constructor<JsonNullableJackson2BeanPropertyWriter> ctor =
                JsonNullableJackson2BeanPropertyWriter.class
                        .getDeclaredConstructor(BeanPropertyWriter.class, PropertyName.class);
        assertTrue(Modifier.isProtected(ctor.getModifiers()));

        // a subclass built on the preserved constructor chain, installed through a
        // module, serializes exactly like the built-in writer with the flag off
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        mapper.registerModule(jackson2SubclassModule());
        assertEquals("{}", mapper.writeValueAsString(new Pet()));
        // of(null) is written as explicit null: the two-argument constructor
        // delegates with allowSuppressNulls = false
        assertEquals("{\"name\":null}",
                mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}",
                mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    @Test
    void testJackson3SubclassKeepsTwoArgConstructorChain() throws Exception {
        // binary compatibility: the protected two-argument constructor still exists
        Constructor<JsonNullableJackson3BeanPropertyWriter> ctor =
                JsonNullableJackson3BeanPropertyWriter.class
                        .getDeclaredConstructor(tools.jackson.databind.ser.BeanPropertyWriter.class,
                                tools.jackson.databind.PropertyName.class);
        assertTrue(Modifier.isProtected(ctor.getModifiers()));

        // a subclass built on the preserved constructor chain, installed through a
        // module, serializes exactly like the built-in writer with the flag off
        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper
                .builder()
                .addModule(new JsonNullableJackson3Module())
                .addModule(jackson3SubclassModule())
                .build();
        assertEquals("{}", mapper.writeValueAsString(new Pet()));
        // of(null) is written as explicit null: the two-argument constructor
        // delegates with allowSuppressNulls = false
        assertEquals("{\"name\":null}",
                mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
        assertEquals("{\"name\":\"Rex\"}",
                mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));
    }

    private static com.fasterxml.jackson.databind.Module jackson2SubclassModule() {
        return new SimpleModule("JsonNullableJackson2SubclassModule") {
            @Override
            public void setupModule(SetupContext context) {
                context.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                                     BeanDescription beanDesc,
                                                                     List<BeanPropertyWriter> beanProperties) {
                        for (int i = 0; i < beanProperties.size(); ++i) {
                            BeanPropertyWriter writer = beanProperties.get(i);
                            if (writer.getType().isTypeOrSubTypeOf(JsonNullable.class)) {
                                beanProperties.set(i, new Jackson2Subclass(writer));
                            }
                        }
                        return beanProperties;
                    }
                });
            }
        };
    }

    private static JacksonModule jackson3SubclassModule() {
        return new JacksonModule() {
            @Override
            public void setupModule(SetupContext context) {
                context.addSerializerModifier(new tools.jackson.databind.ser.ValueSerializerModifier() {
                    @Override
                    public List<tools.jackson.databind.ser.BeanPropertyWriter> changeProperties(
                            tools.jackson.databind.SerializationConfig config,
                            tools.jackson.databind.BeanDescription.Supplier beanDesc,
                            List<tools.jackson.databind.ser.BeanPropertyWriter> beanProperties) {
                        for (int i = 0; i < beanProperties.size(); ++i) {
                            tools.jackson.databind.ser.BeanPropertyWriter writer = beanProperties.get(i);
                            if (writer.getType().isTypeOrSubTypeOf(JsonNullable.class)) {
                                beanProperties.set(i, new Jackson3Subclass(writer));
                            }
                        }
                        return beanProperties;
                    }
                });
            }

            @Override
            public String getModuleName() {
                return "JsonNullableJackson3SubclassModule";
            }

            @Override
            public Version version() {
                return PackageVersion.VERSION;
            }
        };
    }
}
