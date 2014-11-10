package com.fasterxml.jackson.dataformat.protobuf;

import java.util.Arrays;

import com.fasterxml.jackson.core.*;

import junit.framework.TestCase;

abstract class ProtobufTestBase extends TestCase
{
    final protected static String PROTOC_SEARCH_REQUEST = "message SearchRequest {\n"
            +" required string query = 1;\n"
            +" optional int32 page_number = 2;\n"
            +" optional int32 result_per_page = 3;\n"
            +" enum Corpus {\n"
            +"   UNIVERSAL = 0;\n"
            +"   WEB = 1;\n"
            +" }\n"
            +" optional Corpus corpus = 4 [default = UNIVERSAL];\n"
            +"}\n"
    ;
    
    final protected static String PROTOC_BOX =
            "message Point {\n"
            +" required int32 x = 1;\n"
            +" required sint32 y = 2;\n"
            +"}\n"            
            +"message Box {\n"
            +" required Point topLeft = 3;\n"
            +" required Point bottomRight = 5;\n"
            +"}\n"
    ;

    final protected static String PROTOC_NODE =
            "message Node {\n"
            +" required int32 id = 1;\n"
            +" optional Node left = 2;\n"
            +" optional Node right = 3;\n"
            +"}\n"
    ;

    /*
    /**********************************************************
    /* Additional assertion methods
    /**********************************************************
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    protected void assertToken(JsonToken expToken, JsonParser jp)
    {
        assertToken(expToken, jp.getCurrentToken());
    }

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

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
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

}
