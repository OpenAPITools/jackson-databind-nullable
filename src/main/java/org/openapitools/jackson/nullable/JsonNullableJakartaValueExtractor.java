package org.openapitools.jackson.nullable;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.UnwrapByDefault;
import jakarta.validation.valueextraction.ValueExtractor;
import java.util.Collection;

/**
 * Extractor for JsonNullable (modern jakarta-validation version)
 */
@UnwrapByDefault
public class JsonNullableJakartaValueExtractor implements ValueExtractor<JsonNullable<@ExtractedValue ?>> {
    @Override
    public void extractValues(JsonNullable<?> originalValue, ValueReceiver receiver) {
        if (originalValue.isPresent()) {
            Object unwrapped = originalValue.get();
            if (unwrapped instanceof Collection<?>) {
                Collection<?> unwrappedList = (Collection<?>) unwrapped;
                Object[] objects = unwrappedList.toArray();
                for (int i = 0; i < objects.length; i++) {
                    receiver.indexedValue("<list element>", i, objects[i]);
                }
            } else {
                receiver.value(null, originalValue.get());
            }
        }
    }
}
