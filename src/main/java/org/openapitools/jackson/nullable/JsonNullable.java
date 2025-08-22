package org.openapitools.jackson.nullable;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    /**
     * If a value is present, returns the value, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the supplying function
     * @throws NullPointerException if no value is present and the supplying function is null
     *
     * @since 0.2.8
     */
    public T orElseGet(Supplier<? extends T> supplier) {
        return this.isPresent ? this.value : supplier.get();
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * NoSuchElementException.
     *
     * @return the value of this JsonNullable
     * @throws NoSuchElementException if no value if present
     *
     * @since 0.2.8
     */
    public T orElseThrow() {
        if (!isPresent) {
            throw new NoSuchElementException("Value is undefined");
        }
        return value;
    }

    /**
     * If a value is present, returns the value, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @param <X> type of the exception to be thrown
     * @param supplier the supplying function that produces an exception to be
     *        thrown
     * @return the value, if present
     * @throws X if no value is present
     *
     * @since 0.2.8
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> supplier)
        throws X
    {
        if( this.isPresent ) {
            return this.value;
        }
        throw supplier.get();
    }

    /**
     * If a value is present, returns true, otherwise false.
     *
     * @return true if a value is present, otherwise false
     */
    public boolean isPresent() {
        return isPresent;
    }

    /**
     * If a value is not present, returns true, otherwise false.
     *
     * @return true if a value is not present, otherwise false
     *
     * @since 0.2.8
     */
    public boolean isUndefined() {
        return !isPresent;
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws NullPointerException if a value is present and the given action
     *         is null
     */
    public void ifPresent(
            Consumer<? super T> action) {

        if (this.isPresent) {
            action.accept(value);
        }
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise performs the given empty-based action.
     *
     * @param action the action to be performed, if a value is present
     * @param undefinedAction the empty-based action to be performed, if no 
     *        value is present
     * @throws NullPointerException if a value is present and the given action
     *         is null, or no value is present and the given empty-based action
     *         is null
     *
     * @since 0.2.8
     */
    public void ifPresentOrElse( Consumer<? super T> action, Runnable undefinedAction ) {
        if (this.isPresent) {
            action.accept(value);
        }
        else {
            undefinedAction.run();
        }
    }

    /**
     * If a value is present, and the value matches the given predicate, returns
     *  a JsonNullable describing the value, otherwise returns an undefined
     * JsonNullable.
     *
     * @param predicate the predicate to apply to a value, if present
     * @return a JsonNullable describing the value of this JsonNullable,
     *   if a value is present and the value matches the given predicate,
     *   otherwise an undefined JsonNullable
     * @throws NullPointerException if the predicate is null
     *
     * @since 0.2.8
     */
    public JsonNullable<T> filter( Predicate<T> predicate ) {
      if (this.isPresent && predicate.test(value)) {
        return this;
      }
      else {
        return undefined();
      }
    }

    /**
     * If a value is present, returns a JsonNullable describing the result of
     * applying the given mapping function to the value, otherwise returns an
     * undeined JsonNullable.
     *
     * @param <U> the type of the value returned from the mapping function
     * @param mapper the mapping function to apply to a value, if present
     * @return a JsonNullable describing the result of applying a mapping
     *         function to the value of this JsonNullable, if a value is
     *         present, otherwise an undefined JsonNullable
     * @throws NullPointerException if the mapping function is null
     *
     * @since 0.2.8
     */
    public <U> JsonNullable<U> map( Function<T, U> mapper) {
        if (this.isPresent) {
            return new JsonNullable<U>(mapper.apply(value), true);
        }
        return undefined();
    }

    /**
     * If a value is present, returns the result of applying the given
     * JsonNullable-bearing mapping function to the value, otherwise returns an
     * undefined JsonNullable.
     *
     * @param <U> the type of value of the JsonNullable returned by the mapping
     *        function
     * @param mapper the mapping function to apply to a value, if present
     * @return the result of applying a JsonNullable-bearing mapping function to
     *         the value of this JsonNullable, if a value is present, otherwise
     *         an undefined JsonNullable
     * @throws NullPointerException if the mapping function is null or returns a
     *         null result
     *
     * @since 0.2.8
     */
    @SuppressWarnings("unchecked")
    public <U> JsonNullable<U> flatMap( Function<? super T, ? extends JsonNullable<? extends U>> mapper ) {
        if (!this.isPresent) {
            return (JsonNullable<U>)this;
        }

        JsonNullable<U> mapped = (JsonNullable<U>)mapper.apply(value);
        if (mapped == null) {
            throw new NullPointerException("The mapped value is null");
        }
        return mapped;
    }

    /**
     * If a value is present, returns a JsonNullable describing the value,
     * otherwise returns a JsonNullable produced by the supplying function.
     *
     * @param supplier the supplying function that produces a JsonNullable to be
     *        returned
     * @return returns a JsonNullable describing the value of this JsonNullable,
     *         if a value is present, otherwise a JsonNullable produced by the
     *         supplying function.
     * @throws NullPointerException if the supplying function is null or
     *         produces a null result
     *
     * @since 0.2.8
     */
    @SuppressWarnings("unchecked")
    public JsonNullable<T> or( Supplier<? extends JsonNullable<? extends T>> supplier ) {
        if (this.isPresent) {
          return this;
        }
        JsonNullable<T> supplied = (JsonNullable<T>)supplier.get();
        if (supplied == null) {
            throw new NullPointerException("The supplied value is null");
        }
        return supplied;
    }

    /**
     * If a value is present, returns a sequential Stream containing only that
     * value, otherwise returns an empty Stream.
     *
     * @return the JsonNullable value as a Stream
     *
     * @since 0.2.8
     */
    public Stream<T> stream() {
        if (this.isPresent) {
            return Stream.of(value);
        }
        return Stream.empty();
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
