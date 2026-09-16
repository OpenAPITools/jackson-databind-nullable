package org.openapitools.jackson.nullable;

import tools.jackson.core.Version;
import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.JacksonModule;

public class JsonNullableJackson3Module extends JacksonModule {

    private final String NAME = "JsonNullableModule";
    private boolean mapBlankStringToNull = false;

    /**
     * Configures whether blank strings (for example {@code ""} or {@code "  "}) deserialized
     * into a {@code JsonNullable} are mapped to {@code JsonNullable.of(null)} instead of
     * {@code JsonNullable.undefined()}.
     *
     * <p>This matters for PATCH semantics: a blank string sent by a client expresses an
     * explicit intent to clear the value, which {@code undefined()} silently swallows.
     *
     * <p>String-like targets ({@code String}, {@code CharSequence} and its implementations
     * such as {@code StringBuilder}, and {@code Character}) are exempt: for those, a blank
     * string is a legitimate value in its own right and is never diverted to this option.
     * A deserializer the application has registered itself for the property's type is also
     * exempt: it is given the blank string and decides the outcome, and this option does not
     * override whatever that deserializer returns. In other words, the option only takes
     * effect where the blank-string guard still runs, i.e. for non-string-like targets still
     * handled by a deserializer Jackson itself ships.
     *
     * <p>Default is {@code false} for backwards compatibility.
     *
     * @param state {@code true} to map blank strings to {@code JsonNullable.of(null)}
     * @return this module, for chaining
     */
    public JsonNullableJackson3Module mapBlankStringToNull(boolean state) {
        this.mapBlankStringToNull = state;
        return this;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableJackson3Serializers());
        context.addDeserializers(new JsonNullableJackson3Deserializers(mapBlankStringToNull));
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