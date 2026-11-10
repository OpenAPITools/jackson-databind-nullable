package org.openapitools.jackson.nullable;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.io.SerializedString;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.bean.UnwrappingBeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

public class UnwrappingJsonNullableJackson3BeanPropertyWriter extends UnwrappingBeanPropertyWriter {

    /**
     * Whether <code>null</code> values of this property are suppressed
     * (property omitted entirely) when serializing; {@code false} keeps
     * pre-existing behavior where only {@code undefined()} is omitted.
     */
    protected final boolean _allowSuppressNulls;

    public UnwrappingJsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base,
                                                            NameTransformer transformer) {
        this(base, transformer, false);
    }

    public UnwrappingJsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base,
                                                            NameTransformer transformer,
                                                            boolean allowSuppressNulls) {
        super(base, transformer);
        _allowSuppressNulls = allowSuppressNulls;
    }

    protected UnwrappingJsonNullableJackson3BeanPropertyWriter(UnwrappingBeanPropertyWriter base,
                                                               NameTransformer transformer, SerializedString name) {
        this(base, transformer, name,
                (base instanceof UnwrappingJsonNullableJackson3BeanPropertyWriter)
                        && ((UnwrappingJsonNullableJackson3BeanPropertyWriter) base)._allowSuppressNulls);
    }

    protected UnwrappingJsonNullableJackson3BeanPropertyWriter(UnwrappingBeanPropertyWriter base,
                                                               NameTransformer transformer, SerializedString name,
                                                               boolean allowSuppressNulls) {
        super(base, transformer, name);
        _allowSuppressNulls = allowSuppressNulls;
    }

    @Override
    protected UnwrappingBeanPropertyWriter _new(NameTransformer transformer, SerializedString newName) {
        return new UnwrappingJsonNullableJackson3BeanPropertyWriter(this, transformer, newName,
                _allowSuppressNulls);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        if (_allowSuppressNulls && (value == null || ((JsonNullable<?>) value).isNull())) {
            return;
        }
        super.serializeAsProperty(bean, gen, prov);
    }
}
