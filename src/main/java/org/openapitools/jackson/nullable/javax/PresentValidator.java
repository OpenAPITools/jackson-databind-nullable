package org.openapitools.jackson.nullable.javax;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Validator for {@link Present} (classic javax-validation version)
 */
public class PresentValidator implements ConstraintValidator<Present, JsonNullable<?>> {
    @Override
    public boolean isValid(JsonNullable<?> value, ConstraintValidatorContext context) {
        return value != null && value.isPresent();
    }
}
