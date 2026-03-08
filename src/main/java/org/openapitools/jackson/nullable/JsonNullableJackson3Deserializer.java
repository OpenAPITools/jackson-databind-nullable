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


    private boolean isStringDeserializer = false;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    public JsonNullableJackson3Deserializer(JavaType fullType, ValueInstantiator inst,
                                            TypeDeserializer typeDeser, ValueDeserializer<?> deser) {
        super(fullType, inst, typeDeser, deser);
        if (fullType instanceof ReferenceType && ((ReferenceType) fullType).getReferencedType() != null) {
            this.isStringDeserializer = ((ReferenceType) fullType).getReferencedType().isTypeOrSubTypeOf(String.class);
        }
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public JsonNullable<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING && !isStringDeserializer) {
            String str = p.getString().trim();
            if (str.isEmpty()) {
                return JsonNullable.undefined();
            }
        }
        return super.deserialize(p, ctxt);
    }

    @Override
    protected ReferenceTypeDeserializer<JsonNullable<Object>> withResolved(TypeDeserializer typeDeser, ValueDeserializer<?> valueDeser) {
        return new JsonNullableJackson3Deserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
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