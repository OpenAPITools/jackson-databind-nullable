package org.openapitools.jackson.nullable;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;

import javax.validation.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class JsonNullableValueExtractorTest {
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
    public void testUnwrap() {
        final UnitIssue2 unitIssue2 = new UnitIssue2();
        unitIssue2.setRestrictedString("a >15 character long string");
        unitIssue2.setNullableRestrictedString("a >15 character long string");
        unitIssue2.setRestrictedInt(16);
        unitIssue2.setNullableRestrictedInt(16);

        final Set<ConstraintViolation<UnitIssue2>> validate = validator.validate(unitIssue2);

        assertEquals(4, validate.size());
    }

    @Test
    public void testValidationIsNotApplied_whenValueIsUndefined() {
        UnitIssue3 unitIssue = new UnitIssue3();
        Set<ConstraintViolation<UnitIssue3>> violations = validator.validate(unitIssue);
        assertEquals(0, violations.size());
    }

    @Test
    public void testValidationIsAppliedOnDefinedValue_whenNullValueExtracted() {
        UnitIssue3 unitIssue = new UnitIssue3();
        unitIssue.setNotNullString(null);
        Set<ConstraintViolation<UnitIssue3>> violations = validator.validate(unitIssue);
        assertEquals(1, violations.size());
    }
    
    @Test
    public void testCollection() {
        Car aCar = new Car();

        // test for java.util.List
        aCar.addWheel(new Wheel("all"));
        aCar.addWheel(new Wheel());
        aCar.addWheel(new Wheel("some"));
        aCar.addWheel(new Wheel());

        // test for java.util.Set
        aCar.addPerson(new Person());

        Set<ConstraintViolation<Car>> validationResult = validator.validate(aCar);
        assertEquals(3, validationResult.size());
        assertTrue(validationResult.stream().anyMatch(c -> c.getPropertyPath().toString().equals("wheels[1].screws") && c.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().equals("NotNull")));
        assertTrue(validationResult.stream().anyMatch(c -> c.getPropertyPath().toString().equals("wheels[3].screws") && c.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().equals("NotNull")));
        assertTrue(validationResult.stream().anyMatch(c -> c.getPropertyPath().toString().equals("persons[].role") && c.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().equals("NotNull")));
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


    private static class UnitIssue3 {
        @NotNull
        private JsonNullable<String> notNullString = JsonNullable.undefined();

        public void setNotNullString(String value) {
            notNullString = JsonNullable.of(value);
        }
    }


    private static class Car {
        @Valid
        private JsonNullable<List<@Valid Wheel>> wheels = JsonNullable.undefined();

        @Valid
        private JsonNullable<Set<@Valid Person>> persons = JsonNullable.undefined();

        public void addWheel(Wheel wheel) {
            if (wheels == null || !wheels.isPresent()) {
                wheels = JsonNullable.of(new ArrayList<>());
            }
            wheels.get().add(wheel);
        }

        public void addPerson(Person person) {
            if (persons == null || !persons.isPresent()) {
                persons = JsonNullable.of(new HashSet<>());
            }
            persons.get().add(person);
        }
    }

    private static class Wheel {
        @NotNull
        private String screws;

        public Wheel() {
        }

        public Wheel(String screws) {
            this.screws = screws;
        }
    }

    private static class Person {
        @NotNull
        private String role;

        public Person() {
        }

        public Person(String role) {
            this.role = role;
        }
    }

}