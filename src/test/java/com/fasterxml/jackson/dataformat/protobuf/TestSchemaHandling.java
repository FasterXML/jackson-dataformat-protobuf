package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class TestSchemaHandling extends ProtobufTestBase
{
    public void testSimple() throws Exception
    {
        final String SIMPLE_PROTOC = "message SearchRequest {\n"
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

        // First: with implicit first type:
        ProtobufSchema schema = ProtobufSchemaLoader.DEFAULT_INSTANCE.parse(SIMPLE_PROTOC);
        assertNotNull(schema);

        // then with named, step by step
        NativeProtobufSchema nat = ProtobufSchemaLoader.DEFAULT_INSTANCE.parseNative(SIMPLE_PROTOC);
        assertNotNull(nat);
        assertNotNull(nat.forFirstType());
        assertNotNull(nat.forType("SearchRequest"));
    }

    public void testBigger() throws Exception
    {
    }
}
