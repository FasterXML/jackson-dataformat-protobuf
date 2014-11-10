package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class SimpleWriteTest extends ProtobufTestBase
{
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

    final protected static String PROTOC_INT_ARRAY_SPARSE = "message Ints {\n"
            +" repeated sint32 values = 1;\n"
            +"}\n"
    ;

    final protected static String PROTOC_INT_ARRAY_PACKED = "message Ints {\n"
            +" repeated sint32 values = 1 [packed=true];\n"
            +"}\n"
    ;
    
    static class IntArray {
        public int[] values;

        public IntArray(int... v) {
            values = v;
        }
    }
    
    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());

    /*
    /**********************************************************
    /* POJO writes
    /**********************************************************
     */
    
    public void testWritePoint() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX, "Point");
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
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX, "Box");
        schema = schema.withRootType("Box");
        final ObjectWriter w = MAPPER.writerWithType(Box.class)
                .withSchema(schema);
        byte[] bytes = w.writeValueAsBytes(new Box(0x3F, 0x3F, 0x3F, 0x3F));
        assertNotNull(bytes);
        
        // 11 bytes for 2 Points; 4 single-byte ids, 3 x 2-byte values, 1 x 1-byte value
        // but then 2 x 2 bytes for tag, length
        
        for (int i = 0; i < bytes.length; ++i) {
            System.err.println("#"+i+": 0x"+Integer.toHexString(bytes[i] & 0xFF));
        }
      
        assertEquals(11, bytes.length);
    }

    /*
    /**********************************************************
    /* Array writes
    /**********************************************************
     */

    public void testIntArraySparse() throws Exception
    {
        /*
        final protected static String PROTOC_INT_ARRAY = "message Ints {\n"
                +" repeated int32 values = 1;\n"
                +"}\n"
        ;
        */
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_INT_ARRAY_SPARSE);
        final ObjectWriter w = MAPPER.writer(schema);
        byte[] bytes = w.writeValueAsBytes(new IntArray(3, -1, 2));
        // 3 x 2 bytes per value (typed tag, value) -> 6
        assertEquals(6, bytes.length);
    }

    public void testIntArrayPacked() throws Exception
    {
        /*
        final protected static String PROTOC_INT_ARRAY = "message Ints {\n"
                +" repeated int32 values = 1;\n"
                +"}\n"
        ;
        */
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_INT_ARRAY_PACKED);
        final ObjectWriter w = MAPPER.writer(schema);
        byte[] bytes = w.writeValueAsBytes(new IntArray(3, -1, 2));
        // 1 byte for typed tag, 1 byte for length, 3 x 1 byte per value -> 5
        assertEquals(5, bytes.length);
    }
}
