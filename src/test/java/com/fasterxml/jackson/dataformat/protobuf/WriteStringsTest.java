package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class WriteStringsTest extends ProtobufTestBase
{

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());

    public void testSimpleShort() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_NAME);
        final ObjectWriter w = MAPPER.writer(schema);
        byte[] bytes = w.writeValueAsBytes(new Name("Bob", "Burger"));
        assertEquals(13, bytes.length);

        // at main level just seq of fields; first one 1 byte tag, 1 byte len, 3 chars -> 5
        // and second similarly 1 + 1 + 6 -> 8
        assertEquals(13, bytes.length);
        assertEquals(0x12, bytes[0]); // length-prefixed (2), field 2
        assertEquals(3, bytes[1]); // length for array
        assertEquals((byte) 'B', bytes[2]);
        assertEquals((byte) 'o', bytes[3]);
        assertEquals((byte) 'b', bytes[4]);
    
        assertEquals(0x3A, bytes[5]); // length-prefixed (2), field 7
        assertEquals(6, bytes[6]); // length for array
        assertEquals((byte) 'B', bytes[7]);
        assertEquals((byte) 'u', bytes[8]);
        assertEquals((byte) 'r', bytes[9]);
        assertEquals((byte) 'g', bytes[10]);
        assertEquals((byte) 'e', bytes[11]);
        assertEquals((byte) 'r', bytes[12]);
    }

    public void testSimpleLong() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_NAME);
        final ObjectWriter w = MAPPER.writer(schema);
        
        StringBuilder sb = new StringBuilder();
        do {
            sb.append("Bob");
        } while (sb.length() < 128);
        final String LONG_NAME = sb.toString();
        final int longLen = LONG_NAME.length();
        
        byte[] bytes = w.writeValueAsBytes(new Name("Bill", LONG_NAME));
        // 5 bytes for fields (tag, length), 4 for first name, N for second
        assertEquals(9 + longLen, bytes.length);

        // at main level just seq of fields; first one 1 byte tag, 1 byte len, 3 chars -> 5
        // and second similarly 1 + 1 + 6 -> 8
        assertEquals(0x12, bytes[0]);
        assertEquals(4, bytes[1]); // length for array
        assertEquals((byte) 'B', bytes[2]);
        assertEquals((byte) 'i', bytes[3]);
        assertEquals((byte) 'l', bytes[4]);
        assertEquals((byte) 'l', bytes[5]);
    
        assertEquals(0x3A, bytes[6]); // length-prefixed (2), field 7
        assertEquals(128 + (longLen & 0x7F), bytes[7] & 0xFF); // sign set for non-last length bytes
        assertEquals(longLen >> 7, bytes[8]); // no sign bit set
        for (int i = 0; i < longLen; ++i) {
            assertEquals((byte) LONG_NAME.charAt(i), bytes[9+i]);
        }
    }
}
