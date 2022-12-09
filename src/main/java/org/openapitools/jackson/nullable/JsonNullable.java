package org.openapitools.jackson.nullable;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public class JsonNullable<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final JsonNullable<?> UNDEFINED = new JsonNullable<>(null, false);

    private final T value;

    private final boolean isPresent;

    private JsonNullable(T value, boolean isPresent) {
        this.value = value;
        this.isPresent = isPresent;
    }

    /**
     * Create a <code>JsonNullable</code> representing an undefined value (not present).
     *
     * @param <T> a type wildcard
     * @return an empty <code>JsonNullable</code> with no value defined
     */
    public static <T> JsonNullable<T> undefined() {
        @SuppressWarnings("unchecked")
        JsonNullable<T> t = (JsonNullable<T>) UNDEFINED;
        return t;
    }

    /**
     * Create a <code>JsonNullable</code> from the submitted value.
     *
     * @param value the value
     * @param <T>   the type of the value
     * @return the <code>JsonNullable</code> with the submitted value present.
     */
    public static <T> JsonNullable<T> of(T value) {
        return new JsonNullable<>(value, true);
    }

    /**
     * Obtain the value of this <code>JsonNullable</code>.
     *
     * @return the value, if present
     * @throws NoSuchElementException if no value is present
     */
    public T get() {
        if (!isPresent) {
            throw new NoSuchElementException("Value is undefined");
        }
        return value;
    }

    /**
     * Obtain the value of this <code>JsonNullable</code>.
     *
     * @param other the value to be returned if no value is present
     * @return the value of this <code>JsonNullable</code> if present, the submitted value otherwise
     */
    public T orElse(T other) {
        return this.isPresent ? this.value : other;
    }

    public boolean isPresent() {
        return isPresent;
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     */
    public void ifPresent(
            Consumer<? super T> action) {

        if (this.isPresent) {
            action.accept(value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof JsonNullable)) {
            return false;
        }

        JsonNullable<?> other = (JsonNullable<?>) obj;
        return Objects.equals(value, other.value) &&
                isPresent == other.isPresent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, isPresent);
    }

    @Override
    public String toString() {
        return this.isPresent ? String.format("JsonNullable[%s]", value) : "JsonNullable.undefined";
    }
}
