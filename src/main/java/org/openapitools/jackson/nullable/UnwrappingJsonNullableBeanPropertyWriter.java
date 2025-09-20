package org.openapitools.jackson.nullable;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.io.SerializedString;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.bean.UnwrappingBeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

public class UnwrappingJsonNullableBeanPropertyWriter extends UnwrappingBeanPropertyWriter
{
    private static final long serialVersionUID = 1L;

    public UnwrappingJsonNullableBeanPropertyWriter(BeanPropertyWriter base,
                                                    NameTransformer transformer) {
        super(base, transformer);
    }

    protected UnwrappingJsonNullableBeanPropertyWriter(UnwrappingBeanPropertyWriter base,
                                                       NameTransformer transformer, SerializedString name) {
        super(base, transformer, name);
    }

    @Override
    protected UnwrappingBeanPropertyWriter _new(NameTransformer transformer, SerializedString newName)
    {
        return new UnwrappingJsonNullableBeanPropertyWriter(this, transformer, newName);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception
    {
        Object value = get(bean);
        if (JsonNullable.undefined().equals(value) || (_nullSerializer == null && value == null)) {
            return;
        }
        super.serializeAsProperty(bean, gen, prov);
    }
}