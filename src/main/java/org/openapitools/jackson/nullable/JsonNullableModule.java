package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class JsonNullableModule extends Module {

    private final String NAME = "JsonNullableModule";

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableSerializers());
        context.addDeserializers(new JsonNullableDeserializers());
        // Modify type info for JsonNullable
        context.addTypeModifier(new JsonNullableTypeModifier());
        context.addBeanSerializerModifier(new JsonNullableBeanSerializerModifier());
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
