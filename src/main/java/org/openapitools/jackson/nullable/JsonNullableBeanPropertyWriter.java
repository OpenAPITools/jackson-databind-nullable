package org.openapitools.jackson.nullable;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

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
    public void serializeAsProperty(Object bean, JsonGenerator jgen, SerializationContext ctxt) throws Exception
    {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        super.serializeAsProperty(bean, jgen, ctxt);
    }

}
