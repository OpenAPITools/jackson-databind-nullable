package org.openapitools.jackson.nullable;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

public class JsonNullableJackson3ValueSerializerModifier extends ValueSerializerModifier
{
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     Supplier beanDesc,
                                                     List<BeanPropertyWriter> beanProperties)
    {
        for (int i = 0; i < beanProperties.size(); ++i) {
            final BeanPropertyWriter writer = beanProperties.get(i);
            JavaType type = writer.getType();
            if (type.isTypeOrSubTypeOf(JsonNullable.class)) {
                beanProperties.set(i, new JsonNullableJackson3BeanPropertyWriter(writer));
            }
        }
        return beanProperties;
    }
}