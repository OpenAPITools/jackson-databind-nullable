package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Regression guard for the namespace-wide "is this Jackson's own deserializer" check in
// JsonNullableJackson{2,3}Deserializer: Jackson's own java.time deserializers must keep the
// historical empty/blank-string-means-absent shortcut, exactly like Integer/Boolean/enum/plain
// POJOs do (see JsonNullWithEmptyTest), because generated OpenAPI clients commonly wrap
// LocalDate/OffsetDateTime in JsonNullable. A narrower "only jackson-databind's own deser
// package" discriminator would have missed these silently (measured: it turns undefined()
// into a present null, not an exception): on Jackson 2 they ship in the separate
// jackson-datatype-jsr310 module (com.fasterxml.jackson.datatype.jsr310.deser), and on
// Jackson 3 they ship inside jackson-databind itself but as a sibling of, not nested under,
// its deser package (tools.jackson.databind.ext.javatime.deser).
//
// Not parameterized over JsonProcessor: Jackson 2 needs JavaTimeModule registered explicitly
// (jackson-datatype-jsr310, a test-scoped dependency added for this), Jackson 3 does not
// (java.time support ships inside jackson-databind itself, confirmed empirically), so each
// stack needs its own mapper wiring.
class JsonNullWithEmptyJavaTimeTest extends ModuleTestBase {

    static class LocalDateBean {
        public JsonNullable<LocalDate> value;
    }

    static class OffsetDateTimeBean {
        public JsonNullable<OffsetDateTime> value;
    }

    @Test
    void testJackson2JavaTimeWithEmptyAndBlankStillUndefined() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        mapper.registerModule(new JavaTimeModule());

        assertStillUndefined(mapper::readValue);
    }

    // Jackson 3 twin of the test above; no JavaTimeModule to register.
    @Test
    void testJackson3JavaTimeWithEmptyAndBlankStillUndefined() throws Exception {
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .build();

        assertStillUndefined(mapper::readValue);
    }

    private interface Reader {
        Object readValue(String content, Class<?> type) throws Exception;
    }

    private void assertStillUndefined(Reader reader) throws Exception {
        LocalDateBean emptyDate = (LocalDateBean) reader.readValue(aposToQuotes("{'value':''}"), LocalDateBean.class);
        assertNotNull(emptyDate.value);
        assertFalse(emptyDate.value.isPresent());

        LocalDateBean blankDate = (LocalDateBean) reader.readValue(aposToQuotes("{'value':' '}"), LocalDateBean.class);
        assertFalse(blankDate.value.isPresent());

        OffsetDateTimeBean emptyDateTime = (OffsetDateTimeBean) reader.readValue(aposToQuotes("{'value':''}"), OffsetDateTimeBean.class);
        assertFalse(emptyDateTime.value.isPresent());

        OffsetDateTimeBean blankDateTime = (OffsetDateTimeBean) reader.readValue(aposToQuotes("{'value':' '}"), OffsetDateTimeBean.class);
        assertFalse(blankDateTime.value.isPresent());
    }
}
