package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.Module;

public class JsonNullableModule extends Module {

    private final String NAME = "JsonNullableModule";
    private boolean mapBlankStringToNull = false;

    /**
     * Configures whether blank strings (e.g. {@code ""}, {@code "  "}) deserialized into
     * non-String {@code JsonNullable} fields are mapped to {@code JsonNullable.of(null)}
     * instead of {@code JsonNullable.undefined()}.
     *
     * <p>This is relevant for PATCH semantics: a blank string sent by a client expresses
     * explicit intent to clear a value, which {@code undefined()} silently swallows.
     *
     * <p>Default is {@code false} for backwards compatibility.
     */
    public JsonNullableModule mapBlankStringToNull(boolean state) {
        this.mapBlankStringToNull = state;
        return this;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableSerializers());
        context.addDeserializers(new JsonNullableDeserializers(mapBlankStringToNull));
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
