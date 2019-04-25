package org.openapitools.jackson.nullable;

import java.io.Serializable;
import java.util.NoSuchElementException;

public class JsonNullable<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final JsonNullable<?> UNDEFINED = new JsonNullable<Object>(null, false);

    private T value;

    private boolean isPresent;

    private JsonNullable(T value, boolean isPresent) {
        this.value = value;
        this.isPresent = isPresent;
    }

    /**
     * Create a <code>JsonNullable</code> representing an undefined value (not present).
     *
     * @param <T>
     * @return
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
        return new JsonNullable<T>(value, true);
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof JsonNullable)) {
            return false;
        }

        JsonNullable<?> other = (JsonNullable<?>) obj;
        return equals(value, other.value) &&
                equals(isPresent, other.isPresent);
    }

    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    @Override
    public int hashCode() {
        int result = 31 + (value == null ? 0 : value.hashCode());
        Boolean bool1 = Boolean.TRUE;
        bool1.hashCode();
        result = 31 * result + (isPresent ? 1231 : 1237);
        return result;
    }

    @Override
    public String toString() {
        return this.isPresent ? String.format("JsonNullable[%s]", value) : "JsonNullable.undefined";
    }
}
