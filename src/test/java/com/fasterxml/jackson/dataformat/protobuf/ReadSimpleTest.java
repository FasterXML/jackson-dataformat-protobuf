package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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

        // actually let's also try via streaming parser
        JsonParser p = MAPPER.getFactory().createParser(bytes);
        p.setSchema(schema);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("x", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(input.x, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("y", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(input.y, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
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

        assertEquals(15, bytes.length);

        Box result = MAPPER.reader(Box.class).with(schema).readValue(bytes);
        assertNotNull(result);
        assertNotNull(result.topLeft);
        assertNotNull(result.bottomRight);
        assertEquals(input.topLeft, result.topLeft);
        assertEquals(input.bottomRight, result.bottomRight);
    }

    public void testStringArraySimple() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_STRINGS);
        final ObjectWriter w = MAPPER.writerFor(Strings.class)
                .with(schema);
        Strings input = new Strings("Dogs", "like", "Baco\u00F1");
        byte[] bytes = w.writeValueAsBytes(input);
        assertNotNull(bytes);
        assertEquals(20, bytes.length);
        
        Strings result = MAPPER.reader(Strings.class).with(schema).readValue(bytes);
        assertNotNull(result);
        assertNotNull(result.values);
        assertEquals(input.values.length, result.values.length);
        for (int i = 0; i < result.values.length; ++i) {
            assertEquals(input.values[i], result.values[i]);
        }

        // and also verify via streaming
        JsonParser p = MAPPER.getFactory().createParser(bytes);
        p.setSchema(schema);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("values", p.getCurrentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(input.values[0], p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(input.values[1], p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(input.values[2], p.getText());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    public void testStringArrayPacked() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_STRINGS_PACKED);
        final ObjectWriter w = MAPPER.writerFor(Strings.class)
                .with(schema);
        Strings input = new Strings("Dogs", "like", "Baco\u00F1");
        byte[] bytes = w.writeValueAsBytes(input);
        assertNotNull(bytes);
        // one byte less, due to length prefix
        assertEquals(19, bytes.length);

        Strings result = MAPPER.reader(Strings.class).with(schema).readValue(bytes);
        assertNotNull(result);
        assertNotNull(result.values);
        assertEquals(input.values.length, result.values.length);
        for (int i = 0; i < result.values.length; ++i) {
            assertEquals(input.values[i], result.values[i]);
        }
    }
    
    public void testSearchMessage() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_SEARCH_REQUEST);
        final ObjectWriter w = MAPPER.writerFor(SearchRequest.class)
                .with(schema);
        SearchRequest input = new SearchRequest();
        input.corpus = Corpus.WEB;
        input.page_number = 3;
        input.result_per_page = 200;
        input.query = "get all";

        byte[] bytes = w.writeValueAsBytes(input);
        assertNotNull(bytes);
        assertEquals(16, bytes.length);

        SearchRequest result = MAPPER.reader(SearchRequest.class).with(schema).readValue(bytes);
        assertNotNull(result);

        assertEquals(input.page_number, result.page_number);
        assertEquals(input.result_per_page, result.result_per_page);
        assertEquals(input.query, result.query);
        assertEquals(input.corpus, result.corpus);
    }
}
