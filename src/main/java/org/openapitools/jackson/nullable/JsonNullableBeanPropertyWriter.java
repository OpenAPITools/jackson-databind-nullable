package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class JsonNullableBeanPropertyWriter extends BeanPropertyWriter
{
    private static final long serialVersionUID = 1L;

    protected JsonNullableBeanPropertyWriter(BeanPropertyWriter base) {
        super(base);
    }

    protected JsonNullableBeanPropertyWriter(BeanPropertyWriter base, PropertyName newName) {
        super(base, newName);
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new JsonNullableBeanPropertyWriter(this, newName);
    }

    @Override
    public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
        return new UnwrappingJsonNullableBeanPropertyWriter(this, unwrapper);
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
