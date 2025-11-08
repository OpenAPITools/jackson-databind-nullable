package org.openapitools.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;

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

        System.out.println("Success");
    }

    public static final class JsonNullableData {
        public JsonNullable<String> myString;
    }

}
