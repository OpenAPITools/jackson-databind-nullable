package org.openapitools.it;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableValueExtractor;
import org.openapitools.jackson.nullable.javax.Present;

import javax.validation.ConstraintViolation;
import javax.validation.UnexpectedTypeException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import javax.validation.valueextraction.Unwrapping;
import javax.validation.valueextraction.ValueExtractor;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.concurrent.Callable;

public class Main {

    private static int failures;

    public static void main(String[] args) throws Exception {
        tryLoadingJavaxValidation();

        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        Validator validator = factory.getValidator();

        // the existing javax value extractor unwraps JsonNullable
        check("extractor: undefined is not validated", violations(validator, new Sized(JsonNullable.<String>undefined())) == 0);
        check("extractor: null is not validated", violations(validator, new Sized(JsonNullable.<String>of(null))) == 0);
        check("extractor: short value is valid", violations(validator, new Sized(JsonNullable.of("abc"))) == 0);
        check("extractor: long value is unwrapped and rejected", violations(validator, new Sized(JsonNullable.of("too long"))) == 1);

        // @Present on a field
        check("field: undefined is invalid", violations(validator, new OnField(JsonNullable.<String>undefined())) == 1);
        check("field: explicit null is valid", violations(validator, new OnField(JsonNullable.<String>of(null))) == 0);
        check("field: value is valid", violations(validator, new OnField(JsonNullable.of("red"))) == 0);
        check("field: null reference is invalid", violations(validator, new OnField(null)) == 1);
        check("field: default message", "must be present".equals(
                firstMessage(validator, new OnField(JsonNullable.<String>undefined()))));

        // on a getter
        check("getter: undefined is invalid", violations(validator, new OnGetter(JsonNullable.<String>undefined())) == 1);
        check("getter: explicit null is valid", violations(validator, new OnGetter(JsonNullable.<String>of(null))) == 0);
        check("getter: value is valid", violations(validator, new OnGetter(JsonNullable.of("red"))) == 0);
        check("getter: null reference is invalid", violations(validator, new OnGetter(null)) == 1);

        // on a constructor parameter
        Constructor<OnConstructor> constructor = OnConstructor.class.getConstructor(JsonNullable.class);
        check("constructor: undefined is invalid", validator.forExecutables().validateConstructorParameters(
                constructor, new Object[]{JsonNullable.undefined()}).size() == 1);
        check("constructor: explicit null is valid", validator.forExecutables().validateConstructorParameters(
                constructor, new Object[]{JsonNullable.of(null)}).isEmpty());
        check("constructor: value is valid", validator.forExecutables().validateConstructorParameters(
                constructor, new Object[]{JsonNullable.of("red")}).isEmpty());

        // groups, repetition, message
        OnGroup onGroup = new OnGroup(JsonNullable.<String>undefined());
        check("group: not applied in the default group", validator.validate(onGroup).isEmpty());
        check("group: applied in its own group", validator.validate(onGroup, Strict.class).size() == 1);
        OnRepeated onRepeated = new OnRepeated(JsonNullable.<String>undefined());
        check("repeated: one violation per group", validator.validate(onRepeated).size() == 1
                && validator.validate(onRepeated, Default.class, Strict.class).size() == 2);
        check("message: custom message", "color is required".equals(
                firstMessage(validator, new OnMessage(JsonNullable.<String>undefined()))));

        // without the payload the wrapper is never offered to the validator
        check("no payload: undefined passes silently", violations(validator, new WithoutSkip(JsonNullable.<String>undefined())) == 0);
        check("no payload: null reference passes silently", violations(validator, new WithoutSkip(null)) == 0);
        check("no payload: explicit null is refused", refused(validator, new WithoutSkip(JsonNullable.<String>of(null))));
        check("no payload: value is refused", refused(validator, new WithoutSkip(JsonNullable.of("red"))));

        factory.close();
        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("Success");
    }

    private static int violations(Validator validator, Object bean) {
        Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        return violations.size();
    }

    private static String firstMessage(Validator validator, Object bean) {
        Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        return violations.isEmpty() ? null : violations.iterator().next().getMessage();
    }

    private static boolean refused(final Validator validator, final Object bean) throws Exception {
        Callable<Object> validation = new Callable<Object>() {
            @Override
            public Object call() {
                return validator.validate(bean);
            }
        };
        try {
            validation.call();
            return false;
        } catch (UnexpectedTypeException e) {
            return e.getMessage().contains("HV000030");
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "ok      " : "FAILED  ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static void tryLoadingJavaxValidation() {
        for (ValueExtractor<?> ve : java.util.ServiceLoader.load(ValueExtractor.class)) {
            if (ve instanceof JsonNullableValueExtractor) {
                System.out.println("Successfully loaded JsonNullableValueExtractor via ServiceLoader");
                return;
            }
        }
        System.out.println("Failed to load JsonNullableValueExtractor via ServiceLoader");
        System.exit(1);
    }

    public interface Strict {
    }

    public static class Sized {
        @Size(max = 5)
        public JsonNullable<String> value;

        public Sized(JsonNullable<String> value) {
            this.value = value;
        }
    }

    public static class OnField {
        @Present(payload = Unwrapping.Skip.class)
        public JsonNullable<String> color;

        public OnField(JsonNullable<String> color) {
            this.color = color;
        }
    }

    public static class OnGetter {
        private final JsonNullable<String> color;

        public OnGetter(JsonNullable<String> color) {
            this.color = color;
        }

        @Present(payload = Unwrapping.Skip.class)
        public JsonNullable<String> getColor() {
            return color;
        }
    }

    public static class OnConstructor {
        public OnConstructor(@Present(payload = Unwrapping.Skip.class) JsonNullable<String> color) {
        }
    }

    public static class OnGroup {
        @Present(groups = Strict.class, payload = Unwrapping.Skip.class)
        public JsonNullable<String> color;

        public OnGroup(JsonNullable<String> color) {
            this.color = color;
        }
    }

    public static class OnRepeated {
        @Present(payload = Unwrapping.Skip.class)
        @Present(groups = Strict.class, payload = Unwrapping.Skip.class)
        public JsonNullable<String> color;

        public OnRepeated(JsonNullable<String> color) {
            this.color = color;
        }
    }

    public static class OnMessage {
        @Present(message = "color is required", payload = Unwrapping.Skip.class)
        public JsonNullable<String> color;

        public OnMessage(JsonNullable<String> color) {
            this.color = color;
        }
    }

    public static class WithoutSkip {
        @Present
        public JsonNullable<String> color;

        public WithoutSkip(JsonNullable<String> color) {
            this.color = color;
        }
    }
}
