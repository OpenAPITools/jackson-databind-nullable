package org.openapitools.jackson.nullable;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the null-state helpers: ofNull, isNull, isNonNull and ifNotNull.
 */
class JsonNullableNullStateTest {

    @Test
    void ofNullIsPresentWithNullValue() {
        JsonNullable<String> nullable = JsonNullable.ofNull();
        assertTrue(nullable.isPresent());
        assertNull(nullable.get());
        assertEquals(JsonNullable.of(null), nullable);
        assertEquals(JsonNullable.of(null).hashCode(), nullable.hashCode());
        assertEquals("JsonNullable[null]", nullable.toString());
    }

    @Test
    void isNullOnlyForExplicitNull() {
        assertTrue(JsonNullable.ofNull().isNull());
        assertTrue(JsonNullable.of(null).isNull());
        assertFalse(JsonNullable.of("value").isNull());
        assertFalse(JsonNullable.undefined().isNull());
    }

    @Test
    void isNonNullOnlyForNonNullValue() {
        assertTrue(JsonNullable.of("value").isNonNull());
        assertFalse(JsonNullable.ofNull().isNonNull());
        assertFalse(JsonNullable.undefined().isNonNull());
    }

    @Test
    void exactlyOneStateHolds() {
        for (JsonNullable<String> nullable : List.of(
                JsonNullable.<String>undefined(), JsonNullable.<String>ofNull(), JsonNullable.of("value"))) {
            int states = (nullable.isUndefined() ? 1 : 0)
                    + (nullable.isNull() ? 1 : 0)
                    + (nullable.isNonNull() ? 1 : 0);
            assertEquals(1, states, nullable.toString());
        }
    }

    @Test
    void ifNotNullRunsOnlyForNonNullValue() {
        List<Object> seen = new ArrayList<>();
        JsonNullable.of("value").ifNotNull(seen::add);
        JsonNullable.<String>ofNull().ifNotNull(seen::add);
        JsonNullable.<String>undefined().ifNotNull(seen::add);
        assertEquals(List.of("value"), seen);
    }

    @Test
    void ifNotNullAcceptsSupertypeConsumer() {
        List<Object> seen = new ArrayList<>();
        java.util.function.Consumer<Object> consumer = seen::add;
        JsonNullable.of(42).ifNotNull(consumer);
        assertEquals(List.of(42), seen);
    }
}
