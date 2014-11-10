package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class SimpleWriteTest extends ProtobufTestBase
{
    static class Point {
        public int x;
        public int y;

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

    final protected static String PROTOC_POINT_ARRAY_SPARSE = "message Points {\n"
            +" repeated Point points = 1;\n"
            +"}\n"
            +PROTOC_POINT;
    ;

    final protected static String PROTOC_POINT_ARRAY_PACKED = "message Points {\n"
          +" repeated Point points = 1 [packed=true];\n"
          +"}\n"
          +PROTOC_POINT;
  ;
    
    static class IntArray {
        public int[] values;

        public IntArray(int... v) {
            values = v;
        }
    }

    static class PointArray {
        public Point[] points;

        public PointArray(Point... p) {
            points = p;
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
        byte[] bytes = w.writeValueAsBytes(new Box(0x3F, 0x11, 0x18, 0xF));
        assertNotNull(bytes);
        
        // 11 bytes for 2 Points; 4 single-byte ids, 3 x 2-byte values, 1 x 1-byte value
        // but then 2 x 2 bytes for tag, length

        // Root-level has no length-prefix; so we have sequence of Box fields (topLeft, bottomRight)
        // with ids of 3 and 5, respectively.
        // As child messages, they have typed-tag, then VInt-encoded length; lengths are 
        // 4 byte each (typed tag, 1-byte ints)
        // It all adds up to 12 bytes as follows:

        /*
            "message Point {\n"
            +" required int32 x = 1;\n"
            +" required sint32 y = 2;\n"
            +"}\n"            
            +"message Box {\n"
            +" required Point topLeft = 3;\n"
            +" required Point bottomRight = 5;\n"
            +"}\n"
         */

        assertEquals(12, bytes.length);
        
        assertEquals(0x1A, bytes[0]); // wire type 2 (length-prefix), tag id 3
        assertEquals(0x4, bytes[1]); // length, 4 bytes
        assertEquals(0x8, bytes[2]); // wire type 0 (vint), tag id 1
        assertEquals(0x3F, bytes[3]); // vint value, 0x3F remains as is
        assertEquals(0x10, bytes[4]); // wire type 0 (vint), tag id 2
        assertEquals(0x22, bytes[5]); // zig-zagged vint value, 0x11 becomes 0x22

        assertEquals(0x2A, bytes[6]); // wire type 2 (length-prefix), tag id 5
        assertEquals(0x4, bytes[7]); // length, 4 bytes
        assertEquals(0x8, bytes[8]); // wire type 0 (vint), tag id 1
        assertEquals(0x18, bytes[9]); // vint value, 0x18 remains as is
        assertEquals(0x10, bytes[10]); // wire type 0 (vint), tag id 2
        assertEquals(0x1E, bytes[11]); // zig-zagged vint value, 0xF becomes 0x1E
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
                +" repeated int32 values = 1; }\n";
        */
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_INT_ARRAY_SPARSE);
        final ObjectWriter w = MAPPER.writer(schema);
        byte[] bytes = w.writeValueAsBytes(new IntArray(3, -1, 2));
        // 3 x 2 bytes per value (typed tag, value) -> 6
        assertEquals(6, bytes.length);
        assertEquals(0x8, bytes[0]); // zig-zagged vint (0) value, field 1
        assertEquals(0x6, bytes[1]); // zig-zagged value for 3
        assertEquals(0x8, bytes[2]);
        assertEquals(0x1, bytes[3]); // zig-zagged value for -1
        assertEquals(0x8, bytes[4]);
        assertEquals(0x4, bytes[5]); // zig-zagged value for 2
    }

    public void testIntArrayPacked() throws Exception
    {
        final ObjectWriter w = MAPPER.writer(ProtobufSchemaLoader.std.parse(PROTOC_INT_ARRAY_PACKED));
        byte[] bytes = w.writeValueAsBytes(new IntArray(3, -1, 2));
        // 1 byte for typed tag, 1 byte for length, 3 x 1 byte per value -> 5
        assertEquals(5, bytes.length);
        assertEquals(0x8, bytes[0]); // zig-zagged vint (0) value, field 1
        assertEquals(0x3, bytes[1]); // length for array, 3 bytes
        assertEquals(0x6, bytes[2]); // zig-zagged value for 3
        assertEquals(0x1, bytes[3]); // zig-zagged value for -1
        assertEquals(0x4, bytes[4]); // zig-zagged value for 2
    }

    public void testPointArraySparse() throws Exception
    {
        final ObjectWriter w = MAPPER.writer(ProtobufSchemaLoader.std.parse(PROTOC_POINT_ARRAY_SPARSE));
        byte[] bytes = w.writeValueAsBytes(new PointArray(new Point(1, 2), new Point(3, 4)));
        // sequence of 2 embedded messages, each with 1 byte typed tag, 1 byte length
        // and 2 fields of typed-tag and single-byte value
        assertEquals(12, bytes.length);

        assertEquals(0xA, bytes[0]); // wire type 2 (length prefix), id of 1 (-> 0x8)
        assertEquals(4, bytes[1]); // length
        assertEquals(8, bytes[2]); // wire type 0 (3 LSB), id of 1 (-> 0x8)
        assertEquals(1, bytes[3]); // VInt 1, no zig-zag
        assertEquals(0x10, bytes[4]); // wire type 0 (3 LSB), id of 2 (-> 0x10)
        assertEquals(4, bytes[5]); // VInt 2, but with zig-zag

        assertEquals(0xA, bytes[6]); // similar to above
        assertEquals(4, bytes[7]); 
        assertEquals(8, bytes[8]);
        assertEquals(3, bytes[9]); // Point(3, )
        assertEquals(0x10, bytes[10]);
        assertEquals(8, bytes[11]); // Point (, 4)
    }

    public void testPointArrayPacked() throws Exception
    {
        final ObjectWriter w = MAPPER.writer(ProtobufSchemaLoader.std.parse(PROTOC_POINT_ARRAY_PACKED));
        byte[] bytes = w.writeValueAsBytes(new PointArray(new Point(1, 2), new Point(3, 4)));
        // should have 1 byte typed-tag, 1 byte length (for array contents);
        // followed by 2 embedded messages of 5 bytes length

        assertEquals(12, bytes.length);
        assertEquals(0xA, bytes[0]); // length-prefixed (2) value, field 1
        assertEquals(10, bytes[1]); // length of entries in array

        assertEquals(4, bytes[2]); // length of first entry
        assertEquals(8, bytes[3]); // wire type 0 (3 LSB), id of 1 (-> 0x8)
        assertEquals(1, bytes[4]); // VInt 1, no zig-zag
        assertEquals(0x10, bytes[5]); // wire type 0 (3 LSB), id of 2 (-> 0x10)
        assertEquals(4, bytes[6]); // VInt 2, but with zig-zag

        assertEquals(4, bytes[7]); // length of second entry
        assertEquals(8, bytes[8]);
        assertEquals(3, bytes[9]); // Point(3, )
        assertEquals(0x10, bytes[10]);
        assertEquals(8, bytes[11]); // Point (, 4)
    }
}
