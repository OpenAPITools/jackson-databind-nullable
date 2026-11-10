package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class JsonNullableJackson2BeanPropertyWriter extends BeanPropertyWriter
{
    private static final long serialVersionUID = 1L;

    /**
     * Whether <code>null</code> values of this property are suppressed
     * (property omitted entirely) when serializing; {@code false} keeps
     * pre-existing behavior where only {@code undefined()} is omitted.
     */
    protected final boolean _allowSuppressNulls;

    protected JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base) {
        this(base, false);
    }

    protected JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base, boolean allowSuppressNulls) {
        super(base);
        _allowSuppressNulls = allowSuppressNulls;
    }

    /**
     * Constructor variant kept for source and binary compatibility with subclasses
     * written before {@code allowSuppressNulls} was introduced: it mirrors the
     * protected two-argument constructor of Jackson's own {@link BeanPropertyWriter}
     * and delegates to
     * {@link #JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter, PropertyName, boolean)}
     * with {@code allowSuppressNulls} defaulting to {@code false}, preserving the
     * pre-existing serialization behavior.
     */
    protected JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base, PropertyName newName) {
        this(base, newName, false);
    }

    protected JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base, PropertyName newName,
                                                     boolean allowSuppressNulls) {
        super(base, newName);
        _allowSuppressNulls = allowSuppressNulls;
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new JsonNullableJackson2BeanPropertyWriter(this, newName, _allowSuppressNulls);
    }

    @Override
    public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
        return new UnwrappingJsonNullableJackson2BeanPropertyWriter(this, unwrapper, _allowSuppressNulls);
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception
    {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        if (_allowSuppressNulls && (value == null || ((JsonNullable<?>) value).isNull())) {
            return;
        }
        super.serializeAsField(bean, jgen, prov);
    }

}
