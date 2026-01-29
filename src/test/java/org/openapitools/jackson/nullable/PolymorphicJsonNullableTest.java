package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass
@MethodSource("jsonProcessors")
class PolymorphicJsonNullableTest extends ModuleTestBase {
    // For [datatype-jdk8#14]
    public static class Container {
        public JsonNullable<Contained> contained;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "ContainedImpl", value = ContainedImpl.class),
    })
    public static interface Contained {
    }

    public static class ContainedImpl implements Contained {
    }

    @Parameter
    JsonProcessor jsonProcessor;

    @BeforeEach
    void setup() {
        jsonProcessor.mapperWithModule();
    }

    // [datatype-jdk8#14]
    @Test
    void testPolymorphic14() throws Exception {
        final Container dto = new Container();
        dto.contained = JsonNullable.<Contained>of(new ContainedImpl());

        final String json = jsonProcessor.writeValueAsString(dto);

        final Container fromJson = jsonProcessor.readValue(json, Container.class);
        assertNotNull(fromJson.contained);
        assertTrue(fromJson.contained.isPresent());
        assertSame(ContainedImpl.class, fromJson.contained.get().getClass());
    }
}