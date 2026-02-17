package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class JsonNullableJackson2BeanPropertyWriter extends BeanPropertyWriter
{
    private static final long serialVersionUID = 1L;

    protected JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base) {
        super(base);
    }

    protected JsonNullableJackson2BeanPropertyWriter(BeanPropertyWriter base, PropertyName newName) {
        super(base, newName);
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new JsonNullableJackson2BeanPropertyWriter(this, newName);
    }

    @Override
    public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
        return new UnwrappingJsonNullableJackson2BeanPropertyWriter(this, unwrapper);
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception
    {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        super.serializeAsField(bean, jgen, prov);
    }

}
