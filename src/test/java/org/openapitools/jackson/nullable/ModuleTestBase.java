package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

public abstract class ModuleTestBase extends junit.framework.TestCase
{
    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected ObjectMapper mapperWithModule()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper;
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