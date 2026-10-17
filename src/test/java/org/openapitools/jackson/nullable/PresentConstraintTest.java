package org.openapitools.jackson.nullable;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openapitools.jackson.nullable.jakarta.Present;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import jakarta.validation.valueextraction.Unwrapping;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PresentConstraintTest {
    private Validator validator;

    @BeforeAll
    void setUp() {
        try (ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testFieldIsInvalid_whenValueIsUndefined() {
        Set<ConstraintViolation<OnField>> violations = validator.validate(new OnField(JsonNullable.undefined()));
        assertEquals(1, violations.size());
        ConstraintViolation<OnField> violation = violations.iterator().next();
        assertEquals("must be present", violation.getMessage());
        assertEquals("color", violation.getPropertyPath().toString());
        assertEquals(Present.class, violation.getConstraintDescriptor().getAnnotation().annotationType());
    }

    @Test
    void testFieldIsValid_whenValueIsExplicitNull() {
        assertEquals(0, validator.validate(new OnField(JsonNullable.<String>of(null))).size());
    }

    @Test
    void testFieldIsValid_whenValueIsDefined() {
        assertEquals(0, validator.validate(new OnField(JsonNullable.of("red"))).size());
    }

    @Test
    void testFieldIsInvalid_whenReferenceIsNull() {
        Set<ConstraintViolation<OnField>> violations = validator.validate(new OnField(null));
        assertEquals(1, violations.size());
        assertEquals("must be present", violations.iterator().next().getMessage());
    }

    @Test
    void testGetter() {
        Set<ConstraintViolation<OnGetter>> violations = validator.validate(new OnGetter(JsonNullable.undefined()));
        assertEquals(1, violations.size());
        assertEquals("color", violations.iterator().next().getPropertyPath().toString());
        assertEquals(0, validator.validate(new OnGetter(JsonNullable.<String>of(null))).size());
        assertEquals(0, validator.validate(new OnGetter(JsonNullable.of("red"))).size());
        assertEquals(1, validator.validate(new OnGetter(null)).size());
    }

    @Test
    void testConstructorParameter() throws NoSuchMethodException {
        Constructor<OnConstructor> constructor = OnConstructor.class.getConstructor(JsonNullable.class);

        Set<ConstraintViolation<OnConstructor>> violations = validator.forExecutables()
                .validateConstructorParameters(constructor, new Object[]{JsonNullable.undefined()});
        assertEquals(1, violations.size());
        assertEquals("must be present", violations.iterator().next().getMessage());

        assertEquals(0, validator.forExecutables()
                .validateConstructorParameters(constructor, new Object[]{JsonNullable.<String>of(null)}).size());
        assertEquals(0, validator.forExecutables()
                .validateConstructorParameters(constructor, new Object[]{JsonNullable.of("red")}).size());
        assertEquals(1, validator.forExecutables()
                .validateConstructorParameters(constructor, new Object[]{null}).size());
    }

    @Test
    void testGroups() {
        OnGroup undefined = new OnGroup(JsonNullable.undefined());
        assertEquals(0, validator.validate(undefined).size());
        assertEquals(0, validator.validate(undefined, Default.class).size());
        assertEquals(1, validator.validate(undefined, Strict.class).size());
        assertEquals(0, validator.validate(new OnGroup(JsonNullable.<String>of(null)), Strict.class).size());
    }

    @Test
    void testRepeatedThroughList() {
        OnRepeated bean = new OnRepeated(JsonNullable.undefined());
        assertEquals(1, validator.validate(bean).size());
        assertEquals(2, validator.validate(bean, Default.class, Strict.class).size());
        assertEquals(0, validator.validate(new OnRepeated(JsonNullable.of("red")), Default.class, Strict.class).size());
    }

    @Test
    void testCustomMessage() {
        Set<ConstraintViolation<OnMessage>> violations = validator.validate(new OnMessage(JsonNullable.undefined()));
        assertEquals(1, violations.size());
        assertEquals("color is required", violations.iterator().next().getMessage());
    }

    @Test
    void testWithoutSkipUnwrapping_undefinedAndNullReferencePassSilently() {
        // The value extractor unwraps JsonNullable by default and extracts nothing from an undefined one, so the
        // constraint is never evaluated: the check passes although the value is missing.
        assertEquals(0, validator.validate(new WithoutSkip(JsonNullable.undefined())).size());
        assertEquals(0, validator.validate(new WithoutSkip(null)).size());
    }

    @Test
    void testWithoutSkipUnwrapping_definedValueIsRefusedByHibernateValidator() {
        // The validator is looked up for the contained type (String) instead of JsonNullable and not found.
        for (JsonNullable<String> state : Arrays.asList(JsonNullable.<String>of(null), JsonNullable.of("red"))) {
            UnexpectedTypeException exception = assertThrows(UnexpectedTypeException.class,
                    () -> validator.validate(new WithoutSkip(state)));
            assertTrue(exception.getMessage().contains("HV000030: No validator could be found for constraint"
                    + " 'org.openapitools.jackson.nullable.jakarta.Present' validating type 'java.lang.String'"),
                    exception.getMessage());
        }
    }

    private interface Strict {
    }

    private static class OnField {
        @Present(payload = Unwrapping.Skip.class)
        private final JsonNullable<String> color;

        OnField(JsonNullable<String> color) {
            this.color = color;
        }
    }

    private static class OnGetter {
        private final JsonNullable<String> color;

        OnGetter(JsonNullable<String> color) {
            this.color = color;
        }

        @Present(payload = Unwrapping.Skip.class)
        public JsonNullable<String> getColor() {
            return color;
        }
    }

    public static class OnConstructor {
        public final JsonNullable<String> color;

        public OnConstructor(@Present(payload = Unwrapping.Skip.class) JsonNullable<String> color) {
            this.color = color;
        }
    }

    private static class OnGroup {
        @Present(groups = Strict.class, payload = Unwrapping.Skip.class)
        private final JsonNullable<String> color;

        OnGroup(JsonNullable<String> color) {
            this.color = color;
        }
    }

    private static class OnRepeated {
        @Present(payload = Unwrapping.Skip.class)
        @Present(groups = Strict.class, payload = Unwrapping.Skip.class)
        private final JsonNullable<String> color;

        OnRepeated(JsonNullable<String> color) {
            this.color = color;
        }
    }

    private static class OnMessage {
        @Present(message = "color is required", payload = Unwrapping.Skip.class)
        private final JsonNullable<String> color;

        OnMessage(JsonNullable<String> color) {
            this.color = color;
        }
    }

    private static class WithoutSkip {
        @Present
        private final JsonNullable<String> color;

        WithoutSkip(JsonNullable<String> color) {
            this.color = color;
        }
    }
}
