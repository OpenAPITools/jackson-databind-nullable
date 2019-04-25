package org.openapitools.jackson.nullable;

import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JsonNullableValueExtractorTest {

    private Validator validator;

    @Before
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testUnwrap() {
        final UnitIssue2 unitIssue2 = new UnitIssue2();
        unitIssue2.setRestrictedString("a >15 character long string");
        unitIssue2.setNullableRestrictedString("a >15 character long string");
        unitIssue2.setRestrictedInt(Integer.valueOf(16));
        unitIssue2.setNullableRestrictedInt(Integer.valueOf(16));

        final Set<ConstraintViolation<UnitIssue2>> validate = validator.validate(unitIssue2);

        assertEquals(4, validate.size());
    }

    private static class UnitIssue2 {
        @Size(max = 10)
        public String restrictedString;
        @Size(max = 10)
        public JsonNullable<String> nullableRestrictedString;
        @Max(value = 15)
        public Integer restrictedInt;
        @Max(value = 15)
        public JsonNullable<Integer> nullableRestrictedInt;

        public void setRestrictedString(String restrictedString) {
            this.restrictedString = restrictedString;
        }

        public void setNullableRestrictedString(String nullableRestrictedString) {
            this.nullableRestrictedString = JsonNullable.of(nullableRestrictedString);
        }

        public void setRestrictedInt(Integer restrictedInt) {
            this.restrictedInt = restrictedInt;
        }

        public void setNullableRestrictedInt(Integer nullableRestrictedInt) {
            this.nullableRestrictedInt = JsonNullable.of(nullableRestrictedInt);
        }
    }
}