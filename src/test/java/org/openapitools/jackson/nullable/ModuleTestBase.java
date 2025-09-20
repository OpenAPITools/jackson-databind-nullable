package org.openapitools.jackson.nullable;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;

import static org.junit.jupiter.api.Assertions.fail;

abstract class ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected ObjectMapper mapperWithModule()
    {
        return mapperBuilderWithModule().build();
    }

    protected ObjectMapper mapperWithModule(JsonInclude.Include include)
    {
        return mapperBuilderWithModule().changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(include)).build();
    }

    protected JsonMapper.Builder mapperBuilderWithModule()
    {
        return JsonMapper.builder().addModule(new JsonNullableModule());
    }

    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+ Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

    protected String quote(String str) {
        return '"'+str+'"';
    }

    protected String aposToQuotes(String json) {
        return json.replace("'", "\"");
    }
}