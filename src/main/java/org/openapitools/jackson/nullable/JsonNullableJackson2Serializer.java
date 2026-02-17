package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class JsonNullableJackson2Serializer extends ReferenceTypeSerializer<JsonNullable<?>> {

    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Constructors, factory methods
    /**********************************************************
     */

    protected JsonNullableJackson2Serializer(ReferenceType fullType, boolean staticTyping,
                                             TypeSerializer vts, JsonSerializer<Object> ser) {
        super(fullType, staticTyping, vts, ser);
    }

    protected JsonNullableJackson2Serializer(JsonNullableJackson2Serializer base, BeanProperty property,
                                             TypeSerializer vts, JsonSerializer<?> valueSer, NameTransformer unwrapper,
                                             Object suppressableValue)
    {
        // Keep suppressNulls to false to always serialize JsonNullable[null]
        super(base, property, vts, valueSer, unwrapper,
                suppressableValue, false);
    }

    @Override
    protected ReferenceTypeSerializer<JsonNullable<?>> withResolved(BeanProperty prop,
                                                                    TypeSerializer vts, JsonSerializer<?> valueSer,
                                                                    NameTransformer unwrapper)
    {
        return new JsonNullableJackson2Serializer(this, prop, vts, valueSer, unwrapper,
                _suppressableValue);
    }

    @Override
    public ReferenceTypeSerializer<JsonNullable<?>> withContentInclusion(Object suppressableValue,
                                                                         boolean suppressNulls)
    {
        return new JsonNullableJackson2Serializer(this, _property, _valueTypeSerializer,
                _valueSerializer, _unwrapper,
                suppressableValue);
    }

    /*
    /**********************************************************
    /* Abstract method impls
    /**********************************************************
     */

    @Override
    protected boolean _isValuePresent(JsonNullable<?> value) {
        return value.isPresent();
    }

    @Override
    protected Object _getReferenced(JsonNullable<?> value) {
        return value.get();
    }

    @Override
    protected Object _getReferencedIfPresent(JsonNullable<?> value) {
        return value.isPresent() ? value.get() : null;
    }
}
