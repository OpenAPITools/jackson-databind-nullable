package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ParameterizedClass
@MethodSource("jsonProcessors")
class RecordTest extends ModuleTestBase {

    record Patch(JsonNullable<String> name, JsonNullable<Integer> age, String tag) {
    }

    record Batch(List<Patch> patches) {
    }

    @Parameter
    JsonProcessor jsonProcessor;

    @BeforeEach
    void setUp() {
        jsonProcessor.mapperWithModule().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    void testDeserAbsent() throws Exception {
        Patch patch = jsonProcessor.readValue("{}", Patch.class);
        assertEquals(JsonNullable.<String>undefined(), patch.name());
        assertEquals(JsonNullable.<Integer>undefined(), patch.age());
        assertNull(patch.tag());
    }

    @Test
    void testDeserNull() throws Exception {
        Patch patch = jsonProcessor.readValue(aposToQuotes("{'name':null}"), Patch.class);
        assertEquals(JsonNullable.<String>of(null), patch.name());
        assertEquals(JsonNullable.<Integer>undefined(), patch.age());
    }

    @Test
    void testDeserValues() throws Exception {
        Patch patch = jsonProcessor.readValue(aposToQuotes("{'name':'x','age':3}"), Patch.class);
        assertEquals(JsonNullable.of("x"), patch.name());
        assertEquals(JsonNullable.of(3), patch.age());
    }

    @Test
    void testSerUndefined() throws Exception {
        Patch patch = new Patch(JsonNullable.undefined(), JsonNullable.undefined(), null);
        assertEquals("{}", jsonProcessor.writeValueAsString(patch));
    }

    @Test
    void testSerNull() throws Exception {
        Patch patch = new Patch(JsonNullable.of(null), JsonNullable.undefined(), null);
        assertEquals(aposToQuotes("{'name':null}"), jsonProcessor.writeValueAsString(patch));
    }

    @Test
    void testSerValues() throws Exception {
        Patch patch = new Patch(JsonNullable.of("x"), JsonNullable.of(3), "t");
        assertEquals(aposToQuotes("{'name':'x','age':3,'tag':'t'}"), jsonProcessor.writeValueAsString(patch));
    }

    @Test
    void testNestedInList() throws Exception {
        String json = aposToQuotes("{'patches':[{'name':null},{'age':3}]}");
        Batch batch = jsonProcessor.readValue(json, Batch.class);
        assertEquals(Arrays.asList(
                new Patch(JsonNullable.of(null), JsonNullable.undefined(), null),
                new Patch(JsonNullable.undefined(), JsonNullable.of(3), null)), batch.patches());
        assertEquals(json, jsonProcessor.writeValueAsString(batch));
    }
}
