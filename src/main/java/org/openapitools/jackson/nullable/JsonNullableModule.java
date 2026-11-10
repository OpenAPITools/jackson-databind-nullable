package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.Module;

public class JsonNullableModule extends Module {

    private final String NAME = "JsonNullableModule";
    private boolean mapBlankStringToNull = false;
    private boolean allowSuppressNulls = false;

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
     * handled by a deserializer Jackson itself ships. A converter declared with {@code
     * @JsonDeserialize(contentConverter = ...)} on a {@code JsonNullable} property resolves
     * to Jackson's own {@code StdDelegatingDeserializer} for the wrapped value, so a blank
     * string still takes the shortcut above and the converter is not invoked for it;
     * unchanged from earlier versions. A {@code DelegatingDeserializer} an application
     * registers around a type's deserializer, directly or via a {@code
     * BeanDeserializerModifier}, is classified by the innermost deserializer it ultimately
     * wraps rather than by its own class. A deserializer that extends one of Jackson's own
     * concrete deserializers ({@code EnumDeserializer}, {@code BeanDeserializer}, {@code
     * NumberDeserializers.NumberDeserializer}, ...) keeps this shortcut, since it inherits
     * Jackson's own blank-string handling; one that implements deserialization on Jackson's
     * abstract bases ({@code JsonDeserializer}, {@code StdDeserializer}, {@code
     * StdScalarDeserializer}, ...) receives the token instead; and a wrapper that does not
     * extend {@code DelegatingDeserializer} (or does not override {@code getDelegatee()})
     * is classified by its own class rather than by what it wraps, so a wrapper should
     * extend {@code DelegatingDeserializer} to be seen through.
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

    /**
     * Configures whether serialization suppresses <code>null</code> values of
     * <code>JsonNullable</code>-typed bean properties by omitting the property
     * from the output entirely.
     *
     * <p>Without this option a present-but-null value ({@code JsonNullable.of(null)},
     * {@code JsonNullable.ofNull()}) is serialized as an explicit JSON {@code null},
     * even when Jackson's own {@code NON_NULL} / {@code NON_ABSENT} inclusion is
     * configured; only {@code JsonNullable.undefined()} is omitted. With this option
     * enabled, an <code>undefined</code> value, an explicit <code>null</code> value,
     * and a plain <code>null</code> reference of a <code>JsonNullable</code> property
     * are all omitted instead, so no JSON {@code null} is written for them.
     *
     * <p>Suppression happens at object-property level, the same places where
     * {@code JsonNullable.undefined()} was already omitted: root values, array
     * elements and {@code Map} values cannot skip entries mid-stream and therefore
     * keep serializing as JSON {@code null}. Deserialization is not affected.
     *
     * <p>This matters for shared request/response models: an optional property that
     * is set explicitly to clear a value in a PATCH request does not need to appear
     * as a JSON {@code null} in the response body.
     *
     * <p>Default is {@code false} for backwards compatibility: with the default, the
     * serialized output is byte-for-byte identical to earlier versions.
     *
     * @param state {@code true} to omit <code>JsonNullable</code> properties holding
     *              <code>null</code> values from the output
     * @return this module, for chaining
     */
    public JsonNullableModule allowSuppressNulls(boolean state) {
        this.allowSuppressNulls = state;
        return this;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new JsonNullableJackson2Serializers());
        context.addDeserializers(new JsonNullableJackson2Deserializers(mapBlankStringToNull));
        // Modify type info for JsonNullable
        context.addTypeModifier(new JsonNullableJackson2TypeModifier());
        context.addBeanSerializerModifier(new JsonNullableJackson2BeanSerializerModifier(allowSuppressNulls));
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
