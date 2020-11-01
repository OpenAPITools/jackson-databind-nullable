package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

public class JsonNullJacksonServiceLoadingTest extends TestCase {

    public void testJacksonJsonNullableModuleServiceLoading() {
        String foundModuleName = ObjectMapper.findModules().get(0).getModuleName();
        assertEquals(new JsonNullableModule().getModuleName(), foundModuleName);
    }
}