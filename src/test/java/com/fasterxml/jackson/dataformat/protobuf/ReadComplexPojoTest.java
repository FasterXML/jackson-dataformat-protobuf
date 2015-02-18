package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class ReadComplexPojoTest extends ProtobufTestBase
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
        MediaItem input = MediaItem.buildItem();
        byte[] bytes = w.writeValueAsBytes(input);

        assertNotNull(bytes);
        assertEquals(252, bytes.length);

        MediaItem result = MAPPER.reader(MediaItem.class).with(schema)
                .readValue(bytes);
        assertNotNull(result);
        byte[] b2 = w.writeValueAsBytes(result);
        assertEquals(bytes.length, b2.length);

        assertEquals(input, result);
    }
}
