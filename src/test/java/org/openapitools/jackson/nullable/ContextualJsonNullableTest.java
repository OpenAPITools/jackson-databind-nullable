package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextualJsonNullableTest extends ModuleTestBase {
    // [datatypes-java8#17]
    @JsonPropertyOrder({"date", "date1", "date2"})
    static class ContextualJsonNullables {
        public JsonNullable<Date> date;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy+MM+dd")
        public JsonNullable<Date> date1;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy*MM*dd")
        public JsonNullable<Date> date2;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @ParameterizedTest
    @MethodSource("jsonProcessors")
    void testContextualJsonNullables(JsonProcessor jsonProcessor) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        jsonProcessor.mapperWithModule().setDateFormat(df);
        ContextualJsonNullables input = new ContextualJsonNullables();
        input.date = JsonNullable.of(new Date(0L));
        input.date1 = JsonNullable.of(new Date(0L));
        input.date2 = JsonNullable.of(new Date(0L));
        final String json = jsonProcessor.writeValueAsString(input);
        assertEquals(aposToQuotes(
                        "{'date':'1970/01/01','date1':'1970+01+01','date2':'1970*01*01'}"),
                json);
    }
}
