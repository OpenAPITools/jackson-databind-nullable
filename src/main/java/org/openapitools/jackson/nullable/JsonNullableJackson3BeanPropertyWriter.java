package org.openapitools.jackson.nullable;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

public class JsonNullableJackson3BeanPropertyWriter extends BeanPropertyWriter {

    protected JsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base) {
        super(base);
    }

    protected JsonNullableJackson3BeanPropertyWriter(BeanPropertyWriter base, PropertyName newName) {
        super(base, newName);
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new JsonNullableJackson3BeanPropertyWriter(this, newName);
    }

    @Override
    public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
        return new UnwrappingJsonNullableJackson3BeanPropertyWriter(this, unwrapper);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator jgen, SerializationContext ctxt) throws Exception {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        super.serializeAsProperty(bean, jgen, ctxt);
    }

}