package org.openapitools.jackson.nullable;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

public class JsonNullableJackson3BeanPropertyWriter extends BeanPropertyWriter {

    /**
     * Whether <code>null</code> values of this property are suppressed
     * (property omitted entirely) when serializing; {@code false} keeps
     * pre-existing behavior where only {@code undefined()} is omitted.
     */
    protected final boolean _allowSuppressNulls;

    protected JsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base) {
        this(base, false);
    }

    protected JsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base, boolean allowSuppressNulls) {
        super(base);
        _allowSuppressNulls = allowSuppressNulls;
    }

    protected JsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base, PropertyName newName,
                                                     boolean allowSuppressNulls) {
        super(base, newName);
        _allowSuppressNulls = allowSuppressNulls;
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new JsonNullableJackson3BeanPropertyWriter(this, newName, _allowSuppressNulls);
    }

    @Override
    public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
        return new UnwrappingJsonNullableJackson3BeanPropertyWriter(this, unwrapper, _allowSuppressNulls);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator jgen, SerializationContext ctxt) throws Exception {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        if (_allowSuppressNulls && (value == null || ((JsonNullable<?>) value).isNull())) {
            return;
        }
        super.serializeAsProperty(bean, jgen, ctxt);
    }

}
