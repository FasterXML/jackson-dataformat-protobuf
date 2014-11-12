package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class WriteComplexPojoTest extends ProtobufTestBase
{
    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testMediaItemSimple() throws Exception
    {
        /*
        final protected static String PROTOC_INT_ARRAY = "message Ints {\n"
                +" repeated int32 values = 1; }\n";
        */
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_MEDIA_ITEM);
        final ObjectWriter w = MAPPER.writer(schema);
        byte[] bytes = w.writeValueAsBytes(MediaItem.buildItem());

        assertNotNull(bytes);

        assertEquals(246, bytes.length);
        // !!! TODO: verify
    }
}
