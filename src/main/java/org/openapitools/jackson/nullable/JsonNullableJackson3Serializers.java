package org.openapitools.jackson.nullable;


import com.fasterxml.jackson.annotation.JsonFormat.Value;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.type.ReferenceType;

public class JsonNullableJackson3Serializers extends Serializers.Base {
    @Override
    public ValueSerializer<?> findReferenceSerializer(SerializationConfig config,
                                                      ReferenceType refType, Supplier beanDescRef, Value formatOverrides,
                                                      TypeSerializer contentTypeSerializer, ValueSerializer<Object> contentValueSerializer) {
        if (JsonNullable.class.isAssignableFrom(refType.getRawClass())) {
            boolean staticTyping = (contentTypeSerializer == null)
                    && config.isEnabled(MapperFeature.USE_STATIC_TYPING);
            return new JsonNullableJackson3Serializer(refType, staticTyping,
                    contentTypeSerializer, contentValueSerializer);
        }
        return null;
    }
}