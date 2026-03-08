package org.openapitools.jackson.nullable;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ReferenceTypeSerializer;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.util.NameTransformer;

public class JsonNullableJackson3Serializer extends ReferenceTypeSerializer<JsonNullable<?>> {

    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Constructors, factory methods
    /**********************************************************
     */

    protected JsonNullableJackson3Serializer(ReferenceType fullType, boolean staticTyping,
                                             TypeSerializer vts, ValueSerializer<Object> ser) {
        super(fullType, staticTyping, vts, ser);
    }

    protected JsonNullableJackson3Serializer(JsonNullableJackson3Serializer base, BeanProperty property,
                                             TypeSerializer vts, ValueSerializer<?> valueSer, NameTransformer unwrapper,
                                             Object suppressableValue)
    {
        // Keep suppressNulls to false to always serialize JsonNullable[null]
        super(base, property, vts, valueSer, unwrapper,
                suppressableValue, false);
    }

    @Override
    protected ReferenceTypeSerializer<JsonNullable<?>> withResolved(BeanProperty prop,
                                                                    TypeSerializer vts, ValueSerializer<?> valueSer,
                                                                    NameTransformer unwrapper)
    {
        return new JsonNullableJackson3Serializer(this, prop, vts, valueSer, unwrapper,
                _suppressableValue);
    }

    @Override
    public ReferenceTypeSerializer<JsonNullable<?>> withContentInclusion(Object suppressableValue,
                                                                         boolean suppressNulls)
    {
        return new JsonNullableJackson3Serializer(this, _property, _valueTypeSerializer,
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