package org.openapitools.jackson.nullable;


import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.ReferenceTypeDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.ReferenceType;

public class JsonNullableJackson3Deserializer extends ReferenceTypeDeserializer<JsonNullable<Object>> {

    /**
     * Namespace prefix of Jackson 3 itself: not just {@code jackson-databind}'s own
     * {@code deser} package (which, in this generation, is itself split into {@code
     * .std}, {@code .jdk} and {@code .bean} rather than Jackson 2's flatter {@code deser}
     * / {@code deser.std}), but every {@code tools.jackson}-published module an
     * application might add on top of it, e.g. java.time support shipped inside
     * {@code jackson-databind} under {@code tools.jackson.databind.ext.javatime.deser}.
     * Used to tell a deserializer Jackson (in the broad sense, including datatype/other
     * tools.jackson modules) ships from one the application registered itself (directly,
     * via {@code @JsonDeserialize}, or via its own module).
     */
    private static final String JACKSON_NAMESPACE_PREFIX = "tools.jackson.";

    /**
     * True when the referenced type is String-like: {@code String}, {@code CharSequence}
     * (and its implementations, e.g. {@code StringBuilder}) or {@code Character}. For
     * these types an empty/blank JSON string is a legitimate value in its own right, so
     * it must never be special-cased into {@link JsonNullable#undefined()}.
     */
    private boolean isStringLike = false;

    /**
     * Deserializers shipped by Jackson itself (databind, datatype and other tools.jackson
     * modules) keep the historical empty/blank-string-means-absent shortcut below;
     * deserializers from any other namespace, i.e. the application's own or third-party
     * ones, receive the token (see issue #46).
     */
    private boolean isStandardDeserializer = true;

    private final boolean mapBlankStringToNull;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    public JsonNullableJackson3Deserializer(JavaType fullType, ValueInstantiator inst,
                                            TypeDeserializer typeDeser, ValueDeserializer<?> deser) {
        this(fullType, inst, typeDeser, deser, false);
    }

    public JsonNullableJackson3Deserializer(JavaType fullType, ValueInstantiator inst,
                                            TypeDeserializer typeDeser, ValueDeserializer<?> deser,
                                            boolean mapBlankStringToNull) {
        super(fullType, inst, typeDeser, deser);
        this.mapBlankStringToNull = mapBlankStringToNull;
        if (fullType instanceof ReferenceType && ((ReferenceType) fullType).getReferencedType() != null) {
            JavaType referencedType = ((ReferenceType) fullType).getReferencedType();
            this.isStringLike = referencedType.isTypeOrSubTypeOf(CharSequence.class)
                    || referencedType.hasRawClass(Character.class);
        }
        this.isStandardDeserializer = isJacksonOwnDeserializer(deser);
    }

    /**
     * @param deser the resolved delegate (content) deserializer for the referenced type,
     *              or {@code null} if it has not been resolved yet.
     * @return whether {@code deser}'s concrete class lives anywhere under Jackson's own
     * namespace ({@code tools.jackson.}) rather than the application's. This is
     * deliberately namespace-wide, not limited to {@code jackson-databind}'s own {@code
     * deser} package: this generation ships java.time support inside {@code
     * jackson-databind} itself under {@code tools.jackson.databind.ext.javatime.deser},
     * a sibling of {@code deser}, not a sub-package of it, and narrowing the check to
     * {@code deser} would silently change behaviour for every such type, on top of the
     * deliberate change for String-like types. A {@code null} deserializer (not yet
     * resolved) is conservatively treated as standard, preserving this class's
     * pre-existing always-intercept behaviour until the real delegate is known.
     * <p>
     * This deliberately does not use {@code instanceof StdDeserializer} /
     * {@code instanceof StdScalarDeserializer}: both are public base classes that an
     * application's own custom deserializer is free to extend for convenience, and an
     * instanceof check would misclassify such a deserializer as "standard", silently
     * reintroducing the bug tracked as issue #46 for
     * that authoring style. Testing the concrete class's package instead only recognizes
     * deserializers Jackson itself ships, under any of its own packages.
     */
    private static boolean isJacksonOwnDeserializer(ValueDeserializer<?> deser) {
        if (deser == null) {
            return true;
        }
        return deser.getClass().getName().startsWith(JACKSON_NAMESPACE_PREFIX);
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public JsonNullable<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING && !isStringLike && isStandardDeserializer) {
            String str = p.getString().trim();
            if (str.isEmpty()) {
                return mapBlankStringToNull ? JsonNullable.of(null) : JsonNullable.undefined();
            }
        }
        return super.deserialize(p, ctxt);
    }

    @Override
    protected ReferenceTypeDeserializer<JsonNullable<Object>> withResolved(TypeDeserializer typeDeser, ValueDeserializer<?> valueDeser) {
        return new JsonNullableJackson3Deserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser, mapBlankStringToNull);
    }

    @Override
    public Object getAbsentValue(DeserializationContext ctxt) {
        return JsonNullable.undefined();
    }

    @Override
    public JsonNullable<Object> getNullValue(DeserializationContext ctxt) {
        return JsonNullable.of(null);
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return JsonNullable.undefined();
    }

    @Override
    public JsonNullable<Object> referenceValue(Object contents) {
        return JsonNullable.of(contents);
    }

    @Override
    public Object getReferenced(JsonNullable<Object> reference) {
        return reference.get();
    }

    @Override
    public JsonNullable<Object> updateReference(JsonNullable<Object> reference, Object contents) {
        return JsonNullable.of(contents);
    }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        // yes; regardless of value deserializer reference itself may be updated
        return Boolean.TRUE;
    }
}