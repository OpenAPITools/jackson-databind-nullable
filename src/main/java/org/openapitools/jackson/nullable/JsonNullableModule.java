package org.openapitools.jackson.nullable;

import tools.jackson.core.Version;
import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.JacksonModule;

public class JsonNullableModule extends JacksonModule {

    private final String NAME = "JsonNullableModule";

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableSerializers());
        context.addDeserializers(new JsonNullableDeserializers());
        // Modify type info for JsonNullable
        context.addTypeModifier(new JsonNullableTypeModifier());
        context.addSerializerModifier(new JsonNullableValueSerializerModifier());
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
