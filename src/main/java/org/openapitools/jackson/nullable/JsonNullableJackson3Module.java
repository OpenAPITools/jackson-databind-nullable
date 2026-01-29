package org.openapitools.jackson.nullable;

import tools.jackson.core.Version;
import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.JacksonModule;

public class JsonNullableJackson3Module extends JacksonModule {

    private final String NAME = "JsonNullableModule";

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableJackson3Serializers());
        context.addDeserializers(new JsonNullableJackson3Deserializers());
        // Modify type info for JsonNullable
        context.addTypeModifier(new JsonNullableJackson3TypeModifier());
        context.addSerializerModifier(new JsonNullableJackson3ValueSerializerModifier());
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public int hashCode() {
        return NAME.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String getModuleName() {
        return NAME;
    }
}