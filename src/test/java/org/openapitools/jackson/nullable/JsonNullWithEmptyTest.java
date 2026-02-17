package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        };

        public abstract Object getType(JsonProcessor jsonProcessor);
    }

}