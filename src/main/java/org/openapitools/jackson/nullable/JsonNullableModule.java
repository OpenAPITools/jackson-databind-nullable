package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.Module;

public class JsonNullableModule extends Module {

    private final String NAME = "JsonNullableModule";
    private boolean mapBlankStringToNull = false;

    /**
     * Configures whether blank strings (for example {@code ""} or {@code "  "}) deserialized
     * into a non-String {@code JsonNullable} are mapped to {@code JsonNullable.of(null)}
     * instead of {@code JsonNullable.undefined()}.
     *
     * <p>This matters for PATCH semantics: a blank string sent by a client expresses an
     * explicit intent to clear the value, which {@code undefined()} silently swallows.
     * String targets are never affected.
     *
     * <p>Default is {@code false} for backwards compatibility.
     *
     * @param state {@code true} to map blank strings to {@code JsonNullable.of(null)}
     * @return this module, for chaining
     */
    public JsonNullableModule mapBlankStringToNull(boolean state) {
        this.mapBlankStringToNull = state;
        return this;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableJackson2Serializers());
        context.addDeserializers(new JsonNullableJackson2Deserializers(mapBlankStringToNull));
        // Modify type info for JsonNullable
        context.addTypeModifier(new JsonNullableJackson2TypeModifier());
        context.addBeanSerializerModifier(new JsonNullableJackson2BeanSerializerModifier());
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
