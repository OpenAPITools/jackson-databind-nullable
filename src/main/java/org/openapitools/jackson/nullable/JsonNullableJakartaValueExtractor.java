package org.openapitools.jackson.nullable;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.UnwrapByDefault;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Extractor for JsonNullable (modern jakarta-validation version)
 */
@UnwrapByDefault
public class JsonNullableJakartaValueExtractor implements ValueExtractor<JsonNullable<@ExtractedValue ?>> {
    @Override
    public void extractValues(JsonNullable<?> originalValue, ValueReceiver receiver) {
        JsonNullableValueExtractorHelper.extractValues(originalValue, receiver::value);
    }
}
