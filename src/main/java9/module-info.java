module org.openapitools.jackson.nullable {
    requires com.fasterxml.jackson.databind;
    requires static jakarta.validation;
    requires static java.validation;

    exports org.openapitools.jackson.nullable;

    provides com.fasterxml.jackson.databind.Module with org.openapitools.jackson.nullable.JsonNullableModule;
    provides javax.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableValueExtractor;
    provides jakarta.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;
}
