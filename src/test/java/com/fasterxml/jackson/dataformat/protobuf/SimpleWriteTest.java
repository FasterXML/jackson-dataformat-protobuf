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

    final String PROTOC_BOX = PROTOC_POINT
            +"message Box {\n"
            +" required Point topLeft = 1;\n"
            +" required Point bottomRight = 2;\n"
            +"}\n"
    ;

    static class Point {
        public int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Box {
        public Point topLeft, bottomRight;
        
        public Box(int x1, int y1, int x2, int y2) {
            topLeft = new Point(x1, y1);
            bottomRight = new Point(x2, y2);
        }
    }
    
    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());

    public void testWritePoint() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_POINT);
        final ObjectWriter w = MAPPER.writerWithType(Point.class)
                .withSchema(schema);
        byte[] bytes = w.writeValueAsBytes(new Point(7, 2));
        assertNotNull(bytes);
        
        // 4 bytes: 1 byte tags, 1 byte values
        assertEquals(4, bytes.length);
        assertEquals(8, bytes[0]); // wire type 0 (3 LSB), id of 1 (-> 0x8)
        assertEquals(7, bytes[1]); // VInt 7, no zig-zag
        assertEquals(0x10, bytes[2]); // wire type 0 (3 LSB), id of 2 (-> 0x10)
        assertEquals(4, bytes[3]); // VInt 2, but with zig-zag
    }

    public void testWriteCoord() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX);
        schema = schema.withRootType("Box");
        final ObjectWriter w = MAPPER.writerWithType(Box.class)
                .withSchema(schema);
        byte[] bytes = w.writeValueAsBytes(new Box(5, 200, 500, 250));
        assertNotNull(bytes);
        
        // 4 bytes: 1 byte tags, 1 byte values
        assertEquals(4, bytes.length);
        assertEquals(8, bytes[0]); // wire type 0 (3 LSB), id of 1 (-> 0x8)
        assertEquals(7, bytes[1]); // VInt 7, no zig-zag
        assertEquals(0x10, bytes[2]); // wire type 0 (3 LSB), id of 2 (-> 0x10)
        assertEquals(4, bytes[3]); // VInt 2, but with zig-zag
    }
}
