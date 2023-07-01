package org.openapitools.jackson.nullable;

import org.hibernate.validator.messageinterpolation.*;
import org.junit.*;

import javax.validation.*;
import javax.validation.valueextraction.*;
import java.util.*;

import static org.junit.Assert.*;


public class JsonNullablIsPresentValidatorTest {
    private Validator validator;

    @Before
    public void setUp() {
        try (ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    public void testIsValid_whenJsonNullableWithValue() {
        final TestBean bean = new TestBean(JsonNullable.of("value"));

        final Set<ConstraintViolation<TestBean>> validate = validator.validate(bean);

        assertEquals(0, validate.size());
    }

    @Test
    public void testIsValid_whenJsonNullableWithNull() {
        final TestBean bean = new TestBean(JsonNullable.of(null));

        final Set<ConstraintViolation<TestBean>> validate = validator.validate(bean);

        assertEquals(0, validate.size());
    }

    @Test
    public void testIsNotValid_whenJsonNullableUndefined() {
        final TestBean bean = new TestBean(JsonNullable.undefined());

        final Set<ConstraintViolation<TestBean>> validate = validator.validate(bean);

        assertEquals(1, validate.size());
        final ConstraintViolation<TestBean> violation = validate.iterator().next();
        assertEquals("must be present", violation.getMessage());
    }

    @Test
    public void testIsValid_whenNull() {
        final TestBean bean = new TestBean(null);

        final Set<ConstraintViolation<TestBean>> validate = validator.validate(bean);

        assertEquals(0, validate.size());
    }

    static class TestBean {

        @JsonNullableIsPresent(payload = Unwrapping.Skip.class)
        public JsonNullable<String> value;

        public TestBean(JsonNullable<String> value) {
            this.value = value;
        }
    }


}