package org.openapitools.jackson.nullable;

abstract class JsonNullableValueExtractorHelper {
    public static void extractValues(JsonNullable<?> originalValue, ValueSetter valueSetter) {
        if (originalValue.isPresent()) {
            valueSetter.apply(null, originalValue.get());
        }
    }

    @FunctionalInterface
    interface ValueSetter {
        void apply(String var1, Object var2);
    }
}
