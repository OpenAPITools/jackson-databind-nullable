package org.openapitools.jackson.nullable;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;

import java.io.IOException;

public class JsonNullableJackson2Deserializer extends ReferenceTypeDeserializer<JsonNullable<Object>> {

    private static final long serialVersionUID = 1L;

    /**
     * Namespace prefix of Jackson 2 itself: not just {@code jackson-databind}'s own
     * {@code deser} package, but every FasterXML-published module an application might
     * add on top of it, e.g. {@code com.fasterxml.jackson.datatype.jsr310.deser}
     * (java.time support). Used to tell a deserializer Jackson (in the broad sense,
     * including datatype/other FasterXML modules) ships from one the application
     * registered itself (directly, via {@code @JsonDeserialize}, or via its own module).
     */
    private static final String JACKSON_NAMESPACE_PREFIX = "com.fasterxml.jackson.";

    /**
     * Bound on how many {@link JsonDeserializer#getDelegatee()} hops {@link
     * #isJacksonOwnDeserializer} will follow while unwrapping a chain of delegating
     * deserializers, so that a wrapper whose {@code getDelegatee()} misbehaves (e.g.
     * returns itself, or another wrapper in a cycle) cannot loop forever.
     */
    private static final int MAX_DELEGATEE_UNWRAP_HOPS = 16;

    /**
     * True when the referenced type is String-like: {@code String}, {@code CharSequence}
     * (and its implementations, e.g. {@code StringBuilder}) or {@code Character}. For
     * these types an empty/blank JSON string is a legitimate value in its own right, so
     * it must never be special-cased into {@link JsonNullable#undefined()}.
     */
    private boolean isStringLike = false;

    /**
     * Deserializers shipped by Jackson itself (databind, datatype and other FasterXML
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
    public JsonNullableJackson2Deserializer(JavaType fullType, ValueInstantiator inst,
                                            TypeDeserializer typeDeser, JsonDeserializer<?> deser) {
        this(fullType, inst, typeDeser, deser, false);
    }

    public JsonNullableJackson2Deserializer(JavaType fullType, ValueInstantiator inst,
                                            TypeDeserializer typeDeser, JsonDeserializer<?> deser,
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
     * @return whether the innermost deserializer {@code deser} ultimately delegates to
     * (see below) has a concrete class living anywhere under Jackson's own namespace
     * ({@code com.fasterxml.jackson.}) rather than the application's. This is
     * deliberately namespace-wide, not limited to {@code jackson-databind}'s own {@code
     * deser} package: FasterXML's own datatype modules (e.g. {@code jackson-datatype-jsr310}
     * for java.time) ship deserializers outside that package too, and narrowing the check
     * to it would silently change behaviour for every type such a module handles, on top
     * of the deliberate change for String-like types. A {@code null} deserializer (not yet
     * resolved) is conservatively treated as standard, preserving this class's pre-existing
     * always-intercept behaviour until the real delegate is known.
     * <p>
     * Before checking the namespace, this follows {@link JsonDeserializer#getDelegatee()}
     * (bounded by {@link #MAX_DELEGATEE_UNWRAP_HOPS}) to the innermost deserializer of a
     * delegation chain and classifies by that one instead of by {@code deser} itself. A
     * wrapper an application registers around Jackson's own deserializer (a logging or
     * validation decorator, or one a {@code BeanDeserializerModifier} installs around
     * Jackson's {@code BeanDeserializer}) decides nothing about blank strings itself; it
     * only forwards to what it wraps. Classifying by what it wraps instead of by the
     * wrapper keeps the historical blank-string shortcut for it exactly as it was before
     * delegation was considered at all. The trade-off is the mirror image: a {@code
     * DelegatingDeserializer} subclass that special-cases {@code ""} itself, rather than
     * merely forwarding, is now classified by what it wraps rather than by itself, so its
     * own handling of blank strings is bypassed whenever what it wraps is Jackson's own.
     * <p>
     * This deliberately does not use {@code instanceof StdDeserializer} /
     * {@code instanceof StdScalarDeserializer}: both are public base classes that an
     * application's own custom deserializer is free to extend for convenience, and an
     * instanceof check would misclassify such a deserializer as "standard", silently
     * reintroducing the bug tracked as issue #46 for
     * that authoring style. Testing the concrete class's package instead only recognizes
     * deserializers Jackson itself ships, under any of its own packages.
     */
    private static boolean isJacksonOwnDeserializer(JsonDeserializer<?> deser) {
        if (deser == null) {
            return true;
        }
        JsonDeserializer<?> innermost = deser;
        for (int hops = 0; hops < MAX_DELEGATEE_UNWRAP_HOPS; hops++) {
            JsonDeserializer<?> delegatee = innermost.getDelegatee();
            if (delegatee == null) {
                break;
            }
            innermost = delegatee;
        }
        return innermost.getClass().getName().startsWith(JACKSON_NAMESPACE_PREFIX);
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public JsonNullable<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_STRING && !isStringLike && isStandardDeserializer) {
            String str = p.getText().trim();
            if (str.isEmpty()) {
                return mapBlankStringToNull ? JsonNullable.of(null) : JsonNullable.undefined();
            }
        }
        return super.deserialize(p, ctxt);
    }

    @Override
    public JsonNullableJackson2Deserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new JsonNullableJackson2Deserializer(_fullType, _valueInstantiator,
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