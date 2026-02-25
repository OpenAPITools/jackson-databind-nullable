package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

abstract class ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    static Stream<JsonProcessor> jsonProcessors() {
        return Stream.of(
                new Jackson2Processor(),
                new Jackson3Processor()
        );
    }

    protected ObjectMapper mapperWithJackson2Module()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper;
    }

    protected tools.jackson.databind.ObjectMapper mapperWithJackson3Module()
    {
        return mapperBuilderWithJackson3Module().build();
    }

    protected tools.jackson.databind.ObjectMapper mapperWithJackson3Module(JsonInclude.Include include)
    {
        return mapperBuilderWithJackson3Module().changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(include)).build();
    }

    protected JsonMapper.Builder mapperBuilderWithJackson3Module()
    {
        return JsonMapper.builder().addModule(new JsonNullableJackson3Module());
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