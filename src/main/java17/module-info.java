module org.openapitools.jackson.nullable {
    requires static com.fasterxml.jackson.databind;
    requires static tools.jackson.databind;
    requires static jakarta.validation;
    requires static java.validation;

    exports org.openapitools.jackson.nullable;

    // These service providers for optional dependencies can be added, if baseline is 25+ or alternative implementations can be dropped
    // see https://github.com/OpenAPITools/jackson-databind-nullable/issues/100
    // provides com.fasterxml.jackson.databind.Module with org.openapitools.jackson.nullable.JsonNullableModule;
    // provides tools.jackson.databind.JacksonModule with org.openapitools.jackson.nullable.JsonNullableJackson3Module;
    // provides javax.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableValueExtractor;
    // provides jakarta.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;
}
