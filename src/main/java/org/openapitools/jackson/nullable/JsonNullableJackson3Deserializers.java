package org.openapitools.jackson.nullable;

import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.ReferenceType;

public class JsonNullableJackson3Deserializers extends Deserializers.Base {

    private final boolean mapBlankStringToNull;

    public JsonNullableJackson3Deserializers() {
        this(false);
    }

    public JsonNullableJackson3Deserializers(boolean mapBlankStringToNull) {
        this.mapBlankStringToNull = mapBlankStringToNull;
    }

    @Override
    public ValueDeserializer<?> findReferenceDeserializer(ReferenceType refType,
                                                          DeserializationConfig config, Supplier beanDescRef,
                                                          TypeDeserializer contentTypeDeserializer, ValueDeserializer<?> contentDeserializer) {
        return (refType.hasRawClass(JsonNullable.class))
                ? new JsonNullableJackson3Deserializer(refType, null, contentTypeDeserializer, contentDeserializer, mapBlankStringToNull)
                : null;
    }

    @Override
    public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
        return JsonNullable.class.equals(valueType);
    }
}