import org.openapitools.jackson.nullable.JsonNullableModule;
import org.openapitools.jackson.nullable.JsonNullableJackson3Module;

module org.openapitools.jackson.nullable {
    requires static com.fasterxml.jackson.databind;
    requires static tools.jackson.databind;
    requires static jakarta.validation;
    requires static java.validation;

    exports org.openapitools.jackson.nullable;

    provides com.fasterxml.jackson.databind.Module with JsonNullableModule;
    provides tools.jackson.databind.JacksonModule with JsonNullableJackson3Module;
    provides javax.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableValueExtractor;
    provides jakarta.validation.valueextraction.ValueExtractor with org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;
}
