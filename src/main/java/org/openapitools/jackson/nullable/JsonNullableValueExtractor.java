package org.openapitools.jackson.nullable;

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;

/**
 * Extractor for JsonNullable (classic javax-validation version)
 */
@UnwrapByDefault
public class JsonNullableValueExtractor implements ValueExtractor<JsonNullable<@ExtractedValue ?>> {
    @Override
    public void extractValues(JsonNullable<?> originalValue, ValueReceiver receiver) {
        JsonNullableValueExtractorHelper.extractValues(originalValue, receiver::value);
    }
}
