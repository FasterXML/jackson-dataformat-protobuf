package com.fasterxml.jackson.dataformat.protobuf;

import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class WriteSimpleTest extends ProtobufTestBase
{
    static class Point3D extends Point {
        public int z;
        
        public Point3D(int x, int y, int z) {
            super(x, y);
            this.z = z;
        }
    }

    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testWritePoint() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX, "Point");
        final ObjectWriter w = MAPPER.writerFor(Point.class)
                .with(schema);
        byte[] bytes = w.writeValueAsBytes(new Point(7, 2));
        assertNotNull(bytes);

        // 4 bytes: 1 byte tags, 1 byte values
        assertEquals(4, bytes.length);
        assertEquals(8, bytes[0]); // wire type 0 (3 LSB), id of 1 (-> 0x8)
        assertEquals(7, bytes[1]); // VInt 7, no zig-zag
        assertEquals(0x10, bytes[2]); // wire type 0 (3 LSB), id of 2 (-> 0x10)
        assertEquals(4, bytes[3]); // VInt 2, but with zig-zag
    }

    public void testWritePointWithLongs() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_POINT_L);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g = MAPPER.getFactory().createGenerator(bytes);
        g.setSchema(schema);

        g.writeStartObject();
        g.writeFieldName("x");
        g.writeNumber(Long.MAX_VALUE);
        g.writeFieldName("y");
        g.writeNumber(Long.MAX_VALUE);
        g.writeEndObject();
        g.close();
        
        byte[] b = bytes.toByteArray();
        assertNotNull(bytes);

        // 22 bytes: 1 byte tags, 10 byte values
        assertEquals(21, b.length);
        assertEquals(8, b[0]); // wire type 0 (3 LSB), id of 1 (-> 0x8)

        // 7 x 0xFF, last 0x7F -> 0x7F....FF, NOT using zig-zag
        assertEquals(0xFF, b[1] & 0xFF);
        assertEquals(0xFF, b[2] & 0xFF);
        assertEquals(0xFF, b[3] & 0xFF);
        assertEquals(0xFF, b[4] & 0xFF);
        assertEquals(0xFF, b[5] & 0xFF);
        assertEquals(0xFF, b[6] & 0xFF);
        assertEquals(0xFF, b[7] & 0xFF);
        assertEquals(0xFF, b[8] & 0xFF);
        assertEquals(0x7F, b[9] & 0x7F);

        assertEquals(0x10, b[10]); // wire type 0 (3 LSB), id of 2 (-> 0x10)

        // but 'y' will be using zig-zag
        assertEquals(0xFE, b[11] & 0xFF);
        assertEquals(0xFF, b[12] & 0xFF);
        assertEquals(0xFF, b[13] & 0xFF);
        assertEquals(0xFF, b[14] & 0xFF);
        assertEquals(0xFF, b[15] & 0xFF);
        assertEquals(0xFF, b[16] & 0xFF);
        assertEquals(0xFF, b[17] & 0xFF);
        assertEquals(0xFF, b[18] & 0xFF);
        assertEquals(0xFF, b[19] & 0xFF);
        assertEquals(0x01, b[20] & 0x01);
    }
    
    public void testWriteCoord() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX, "Box");
        schema = schema.withRootType("Box");
        final ObjectWriter w = MAPPER.writerFor(Box.class)
                .with(schema);
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

    public void testUnknownProperties() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX, "Point");
        final ObjectWriter w = MAPPER.writerFor(Point3D.class)
                .with(schema);
        
        // First: if disabled, should get an error
        try {
            /*byte[] bytes =*/ w
                .without(JsonGenerator.Feature.IGNORE_UNKNOWN)
                .writeValueAsBytes(new Point3D(1, 2, 3));
        } catch (JsonProcessingException e) {
            verifyException(e, "Unrecognized field 'z'");
        }

        byte[] bytes = w
                .with(JsonGenerator.Feature.IGNORE_UNKNOWN)
                .writeValueAsBytes(new Point3D(1, 2, 3));
        assertNotNull(bytes);
        assertNotNull(bytes);
        assertEquals(4, bytes.length);
    }
}
