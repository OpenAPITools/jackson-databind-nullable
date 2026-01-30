import org.openapitools.jackson.nullable.JsonNullableModule;

module org.openapitools.jackson.nullable {
    requires com.fasterxml.jackson.databind;
    requires static jakarta.validation;
    requires static java.validation;

    exports org.openapitools.jackson.nullable;

    provides com.fasterxml.jackson.databind.Module with org.openapitools.jackson.nullable.JsonNullableModule;
    // These service providers for optional dependencies can be added, if baseline is 25+ or javax support is dropped
    // see https://github.com/OpenAPITools/jackson-databind-nullable/issues/100
    //provides javax.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableValueExtractor;
    //provides jakarta.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;
}
