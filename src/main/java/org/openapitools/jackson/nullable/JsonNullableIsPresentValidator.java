package org.openapitools.jackson.nullable;

import javax.validation.*;

public class JsonNullableIsPresentValidator implements ConstraintValidator<JsonNullableIsPresent, JsonNullable<?>> {
    @Override
    public void initialize(JsonNullableIsPresent constraintAnnotation) {
    }

    @Override
    public boolean isValid(JsonNullable<?> jsonNullable, ConstraintValidatorContext constraintValidatorContext) {
        if (jsonNullable == null) {
            return true;
        }
        return jsonNullable.isPresent();
    }
}