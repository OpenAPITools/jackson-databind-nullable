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

import java.lang.reflect.Modifier;

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
     * This library's own deserializer class, matched by identity rather than by package
     * prefix: a package-prefix match would claim any class placed in this package,
     * including this project's own tests and an application's own. Identity matches
     * exactly this class, which is what the content deserializer of a nested {@code
     * JsonNullable} is, e.g. {@code JsonNullable<JsonNullable<String>>}. This library's
     * own blank-string handling is what the outer {@code JsonNullable} should defer to.
     */
    private static final Class<?> OWN_DESERIALIZER_CLASS = JsonNullableJackson3Deserializer.class;

    /**
     * Bound on how many {@link ValueDeserializer#getDelegatee()} hops {@link
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
     * @return whether the innermost deserializer {@code deser} ultimately delegates to
     * (see below) is classified as Jackson's own: walking up from its concrete class to
     * the nearest ancestor whose class name lives under Jackson's own namespace ({@code
     * tools.jackson.}) or is this library's own class ({@link #OWN_DESERIALIZER_CLASS}), that
     * ancestor being this library's own class or a concrete Jackson class counts as
     * standard, an abstract Jackson class counts as application-owned (see below for the
     * full rule). This is deliberately namespace-wide, not limited to {@code
     * jackson-databind}'s own {@code deser} package: this generation ships java.time
     * support inside {@code jackson-databind} itself under {@code
     * tools.jackson.databind.ext.javatime.deser}, a sibling of {@code deser}, not a
     * sub-package of it, and narrowing the check to {@code deser} would silently change
     * behaviour for every such type, on top of the deliberate change for String-like
     * types. A {@code null} deserializer (not yet resolved) is conservatively treated as
     * standard, preserving this class's pre-existing always-intercept behaviour until the
     * real delegate is known.
     * <p>
     * Before walking the class hierarchy, this follows {@link ValueDeserializer#getDelegatee()}
     * (bounded by {@link #MAX_DELEGATEE_UNWRAP_HOPS}) to the innermost deserializer of a
     * delegation chain and classifies by that one instead of by {@code deser} itself. A
     * wrapper an application registers around Jackson's own deserializer (a logging or
     * validation decorator, or one a {@code ValueDeserializerModifier} installs around
     * Jackson's {@code BeanDeserializer}) decides nothing about blank strings itself; it
     * only forwards to what it wraps. Classifying by what it wraps instead of by the
     * wrapper keeps the historical blank-string shortcut for it exactly as it was before
     * delegation was considered at all. The trade-off is the mirror image: a {@code
     * DelegatingDeserializer} subclass that special-cases {@code ""} itself, rather than
     * merely forwarding, is now classified by what it wraps rather than by itself, so its
     * own handling of blank strings is bypassed whenever what it wraps is Jackson's own. A
     * wrapper that does not extend {@code DelegatingDeserializer} (or does not override
     * {@code getDelegatee()}) is not seen through this way and is classified by its own
     * class instead.
     * <p>
     * From the innermost deserializer's concrete class, this walks up the superclass chain
     * (the class itself included) to the nearest ancestor whose name starts with either
     * prefix above:
     * <ul>
     * <li>no such ancestor exists (a class implementing the deserializer interface from
     * scratch without extending anything of Jackson's or this library's): application-owned.
     * On Jackson 3 this branch is unreachable, since {@code ValueDeserializer} itself is an
     * abstract class under Jackson's own namespace and therefore always matches, but it is
     * kept for safety.
     * <li>the ancestor is this library's own class (nested {@code JsonNullable}, e.g.
     * {@code JsonNullable<JsonNullable<String>>}, whose content deserializer is another
     * instance of this class): standard, since this library's own blank-string handling is
     * exactly what the outer {@code JsonNullable} should defer to.
     * <li>the ancestor is one of Jackson's own classes and is <em>abstract</em> (e.g.
     * {@code ValueDeserializer}, {@code StdDeserializer}, {@code StdScalarDeserializer}):
     * application-owned. Extending an abstract base and implementing the actual
     * deserialization is exactly the #46 authoring style, and it must keep receiving the
     * token regardless of how many abstract layers of Jackson's own it is built on.
     * <li>the ancestor is one of Jackson's own classes and is <em>concrete</em> (e.g.
     * {@code EnumDeserializer}, {@code BeanDeserializer}, {@code
     * NumberDeserializers.NumberDeserializer}): standard, whether or not the subclass
     * overrides {@code deserialize()}. The shortcut runs before the subclass is ever
     * reached, exactly as it did for that concrete Jackson deserializer before delegation
     * was considered at all; to handle blank strings itself, a deserializer extends one of
     * Jackson's abstract bases instead (the bullet above).
     * </ul>
     * <p>
     * This deliberately does not use a bare {@code instanceof StdDeserializer} /
     * {@code instanceof StdScalarDeserializer} check: both are public base classes that an
     * application's own custom deserializer is free to extend for convenience, and treating
     * every instance as "standard" would misclassify such a deserializer, silently
     * reintroducing the bug tracked as issue #46 for that authoring style. The
     * abstract/concrete distinction above is exactly what keeps that authoring style
     * application-owned: {@code StdDeserializer} and {@code StdScalarDeserializer} are
     * themselves abstract, so an application class built directly on either one still
     * receives the token, while a subclass of one of Jackson's own concrete deserializers
     * (which has already implemented {@code deserialize()} itself) does not.
     */
    private static boolean isJacksonOwnDeserializer(ValueDeserializer<?> deser) {
        if (deser == null) {
            return true;
        }
        ValueDeserializer<?> innermost = deser;
        for (int hops = 0; hops < MAX_DELEGATEE_UNWRAP_HOPS; hops++) {
            ValueDeserializer<?> delegatee = innermost.getDelegatee();
            if (delegatee == null) {
                break;
            }
            innermost = delegatee;
        }
        for (Class<?> ancestor = innermost.getClass(); ancestor != null; ancestor = ancestor.getSuperclass()) {
            String name = ancestor.getName();
            if (ancestor == OWN_DESERIALIZER_CLASS) {
                return true;
            }
            if (name.startsWith(JACKSON_NAMESPACE_PREFIX)) {
                return !Modifier.isAbstract(ancestor.getModifiers());
            }
        }
        return false;
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