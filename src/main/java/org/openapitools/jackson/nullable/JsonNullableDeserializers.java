package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;

public class JsonNullableDeserializers extends Deserializers.Base {

    private final boolean mapBlankStringToNull;

    public JsonNullableDeserializers(boolean mapBlankStringToNull) {
        this.mapBlankStringToNull = mapBlankStringToNull;
    }

    @Override
    public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
                                                         DeserializationConfig config, BeanDescription beanDesc,
                                                         TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
        return (refType.hasRawClass(JsonNullable.class))
            ? new JsonNullableDeserializer(refType, null, contentTypeDeserializer, contentDeserializer, mapBlankStringToNull)
            : null;
    }
}
