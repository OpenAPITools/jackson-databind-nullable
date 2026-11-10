package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class UnwrappingJsonNullableJackson2BeanPropertyWriter extends UnwrappingBeanPropertyWriter
{
    private static final long serialVersionUID = 1L;

    /**
     * Whether <code>null</code> values of this property are suppressed
     * (property omitted entirely) when serializing; {@code false} keeps
     * pre-existing behavior where only {@code undefined()} is omitted.
     */
    protected final boolean _allowSuppressNulls;

    public UnwrappingJsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base,
                                                            NameTransformer transformer) {
        this(base, transformer, false);
    }

    public UnwrappingJsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base,
                                                            NameTransformer transformer,
                                                            boolean allowSuppressNulls) {
        super(base, transformer);
        _allowSuppressNulls = allowSuppressNulls;
    }

    protected UnwrappingJsonNullableJackson2BeanPropertyWriter(UnwrappingBeanPropertyWriter base,
                                                               NameTransformer transformer, SerializedString name) {
        this(base, transformer, name,
                (base instanceof UnwrappingJsonNullableJackson2BeanPropertyWriter)
                        && ((UnwrappingJsonNullableJackson2BeanPropertyWriter) base)._allowSuppressNulls);
    }

    protected UnwrappingJsonNullableJackson2BeanPropertyWriter(UnwrappingBeanPropertyWriter base,
                                                               NameTransformer transformer, SerializedString name,
                                                               boolean allowSuppressNulls) {
        super(base, transformer, name);
        _allowSuppressNulls = allowSuppressNulls;
    }

    @Override
    protected UnwrappingBeanPropertyWriter _new(NameTransformer transformer, SerializedString newName)
    {
        return new UnwrappingJsonNullableJackson2BeanPropertyWriter(this, transformer, newName,
                _allowSuppressNulls);
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception
    {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        if (_allowSuppressNulls && (value == null || ((JsonNullable<?>) value).isNull())) {
            return;
        }
        super.serializeAsField(bean, gen, prov);
    }
}
