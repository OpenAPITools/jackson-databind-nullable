package org.openapitools.jackson.nullable.jakarta;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated {@code JsonNullable} must be present, that is not undefined. An explicit {@code null} value is
 * present and therefore valid; a {@code null} reference is not.
 * <p>
 * The value extractor of this module unwraps {@code JsonNullable} by default, so the constraint only sees the
 * wrapper when unwrapping is skipped at the place of use:
 * <pre>
 * &#64;Present(payload = Unwrapping.Skip.class)
 * private JsonNullable&lt;String&gt; color = JsonNullable.undefined();
 * </pre>
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Repeatable(Present.List.class)
@Documented
@Constraint(validatedBy = { PresentValidator.class })
public @interface Present {

    String message() default "must be present";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    /**
     * Defines several {@link Present} annotations on the same element.
     */
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @interface List {

        Present[] value();
    }
}
