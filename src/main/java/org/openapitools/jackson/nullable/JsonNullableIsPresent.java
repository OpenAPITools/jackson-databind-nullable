package org.openapitools.jackson.nullable;

import javax.validation.*;
import java.lang.annotation.*;

@Constraint(validatedBy = JsonNullableIsPresentValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonNullableIsPresent {
    String message() default "must be present";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}