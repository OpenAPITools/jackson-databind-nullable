package org.openapitools.jackson.nullable;


import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class JsonNullableDeserializer extends ReferenceTypeDeserializer<JsonNullable<Object>> {

    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    public JsonNullableDeserializer(JavaType fullType, ValueInstantiator inst,
                                    TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(fullType, inst, typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public JsonNullableDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new JsonNullableDeserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
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