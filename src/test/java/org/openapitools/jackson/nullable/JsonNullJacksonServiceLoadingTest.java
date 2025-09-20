package org.openapitools.jackson.nullable;

import tools.jackson.databind.cfg.MapperBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonNullJacksonServiceLoadingTest {

    @Test
    void testJacksonJsonNullableModuleServiceLoading() {
        String foundModuleName = MapperBuilder.findModules().get(0).getModuleName();
        assertEquals(new JsonNullableModule().getModuleName(), foundModuleName);
    }
}