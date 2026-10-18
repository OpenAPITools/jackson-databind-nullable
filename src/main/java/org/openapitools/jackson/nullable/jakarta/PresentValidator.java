package org.openapitools.jackson.nullable.jakarta;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Validator for {@link Present} (modern jakarta-validation version)
 */
public class PresentValidator implements ConstraintValidator<Present, JsonNullable<?>> {
    @Override
    public boolean isValid(JsonNullable<?> value, ConstraintValidatorContext context) {
        return value != null && value.isPresent();
    }
}
