package org.openapitools.jackson.nullable;

import java.util.Collection;

abstract class JsonNullableValueExtractorHelper {
    public static void extractValues(JsonNullable<?> originalValue, IndexedValueSetter indexedValueSetter, ValueSetter valueSetter) {
        if (originalValue.isPresent()) {
            Object unwrapped = originalValue.get();
            if (unwrapped instanceof Collection<?>) {
                Collection<?> unwrappedList = (Collection<?>) unwrapped;
                Object[] objects = unwrappedList.toArray();
                for (int i = 0; i < objects.length; i++) {
                    indexedValueSetter.apply("<list element>", i, objects[i]);
                }
            } else {
                valueSetter.apply(null, originalValue.get());
            }
        }
    }

    @FunctionalInterface
    interface IndexedValueSetter {
        void apply(String var1, int var2, Object var3);
    }

    @FunctionalInterface
    interface ValueSetter {
        void apply(String var1, Object var2);
    }
}
