package org.openapitools.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;
import jakarta.validation.valueextraction.ValueExtractor;

import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) throws Exception {
        JsonNullableJakartaValueExtractor extractor = new JsonNullableJakartaValueExtractor();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        JsonNullableData data = mapper.readValue("{\"myString\":\"simpleString\"}",
                JsonNullableData.class);

        if (data.myString.isUndefined()) {
            System.exit(1);
        }
        if (!data.myString.get().equals("simpleString")) {
            System.exit(1);
        }

        tryLoadingJakartaValidation();
        System.out.println("Success");
    }

    public static final class JsonNullableData {
        public JsonNullable<String> myString;
    }

    private static void tryLoadingJakartaValidation() {
        ServiceLoader<ValueExtractor> loaded = ServiceLoader.load(ValueExtractor.class);
        for (ValueExtractor ve : loaded) {
            if (ve instanceof JsonNullableJakartaValueExtractor) {
                System.out.println("Successfully loaded JsonNullableJakartaValueExtractor via ServiceLoader");
                return;
            }
        }
        System.out.println("Failed to load JsonNullableJakartaValueExtractor via ServiceLoader");
        System.exit(1);
    }

}
