package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class ReadSimpleTest extends ProtobufTestBase
{
    final ObjectMapper MAPPER = new ObjectMapper(new ProtobufFactory());

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testReadPoint() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX, "Point");
        final ObjectWriter w = MAPPER.writerFor(Point.class)
                .with(schema);
        Point input = new Point(151, -444);
        byte[] bytes = w.writeValueAsBytes(input);
        assertNotNull(bytes);

        // 6 bytes: 1 byte tags, 2 byte values
        assertEquals(6, bytes.length);

        // but more importantly, try to parse
        Point result = MAPPER.reader(Point.class).with(schema).readValue(bytes);
        assertNotNull(result);
        assertEquals(input.x, result.x);
        assertEquals(input.y, result.y);
    }

    public void testReadName() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_NAME);
        final ObjectWriter w = MAPPER.writerFor(Name.class)
                .with(schema);
        // make sure to use at least one non-ascii char in there:
        Name input = new Name("Billy", "Baco\u00F1");

        byte[] bytes = w.writeValueAsBytes(input);
        assertNotNull(bytes);

        assertEquals(15, bytes.length);

        Name result = MAPPER.reader(Name.class).with(schema).readValue(bytes);
        assertNotNull(result);
        assertEquals(input.first, result.first);
        assertEquals(input.last, result.last);
    }

    public void testReadBox() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX);
        final ObjectWriter w = MAPPER.writerFor(Box.class)
                .with(schema);
        Point topLeft = new Point(100, 150);
        Point bottomRight = new Point(500, 1090);
        Box input = new Box(topLeft, bottomRight);

        byte[] bytes = w.writeValueAsBytes(input);
        assertNotNull(bytes);

        // 6 bytes: 1 byte tags, 2 byte values
        assertEquals(15, bytes.length);

        /*
for (int i = 0; i < bytes.length; ++i) {
    System.out.printf("#%d: 0x%x\n", i, bytes[i] & 0xFF);
}
*/

        Box result = MAPPER.reader(Box.class).with(schema).readValue(bytes);
        assertNotNull(result);
        assertNotNull(result.topLeft);
        assertNotNull(result.bottomRight);
        assertEquals(input.topLeft, result.topLeft);
        assertEquals(input.bottomRight, result.bottomRight);
    }

}
