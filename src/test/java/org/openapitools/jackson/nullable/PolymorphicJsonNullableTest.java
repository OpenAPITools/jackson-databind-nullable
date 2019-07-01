package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PolymorphicJsonNullableTest extends ModuleTestBase
{
    // For [datatype-jdk8#14]
    public static class Container {
        public JsonNullable<Contained> contained;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "ContainedImpl", value = ContainedImpl.class),
    })
    public static interface Contained { }

    public static class ContainedImpl implements Contained { }

    private final ObjectMapper MAPPER = mapperWithModule();

    // [datatype-jdk8#14]
    public void testPolymorphic14() throws Exception
    {
        final Container dto = new Container();
        dto.contained = JsonNullable.<Contained>of(new ContainedImpl());

        final String json = MAPPER.writeValueAsString(dto);

        final Container fromJson = MAPPER.readValue(json, Container.class);
        assertNotNull(fromJson.contained);
        assertValueIsPresent(fromJson.contained );
        assertSame(ContainedImpl.class, fromJson.contained.get().getClass());
    }
}