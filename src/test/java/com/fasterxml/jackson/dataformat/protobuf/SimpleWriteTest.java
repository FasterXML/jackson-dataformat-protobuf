package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class SimpleWriteTest extends ProtobufTestBase
{
    final String PROTOC_POINT = "message Point {\n"
            +" required int32 x = 1;\n"
            +" required sint32 y = 2;\n"
            +"}\n"
    ;

    static class Point {
        public int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    final ProtobufSchema POINT_SCHEMA;
    {
        ProtobufSchema ps = null;
        try {
            ps = ProtobufSchemaLoader.DEFAULT_INSTANCE.parse(PROTOC_POINT);
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            POINT_SCHEMA = ps;
        }
    }

    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());
    
    public void testWritePoint() throws Exception
    {
        final ObjectWriter w = MAPPER.writerWithType(Point.class)
                .withSchema(POINT_SCHEMA);
        byte[] bytes = w.writeValueAsBytes(new Point(7, 2));
        assertNotNull(bytes);
        
        // 4 bytes: 1 byte tags, 1 byte values
        assertEquals(4, bytes.length);
        assertEquals(8, bytes[0]); // wire type 0 (3 LSB), id of 1 (-> 0x8)
        assertEquals(7, bytes[1]); // VInt 7, no zig-zag
        assertEquals(0x10, bytes[2]); // wire type 0 (3 LSB), id of 2 (-> 0x10)
        assertEquals(4, bytes[3]); // VInt 2, but with zig-zag
    }
}
