package com.fasterxml.jackson.dataformat.protobuf;

import java.util.List;

import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufField;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class TestSchemaHandling extends ProtobufTestBase
{
    public void testSimpleSearchRequest() throws Exception
    {
        // First: with implicit first type:
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_SEARCH_REQUEST);
        assertNotNull(schema);

        // then with named, step by step
        NativeProtobufSchema nat = ProtobufSchemaLoader.std.parseNative(PROTOC_SEARCH_REQUEST);
        assertNotNull(nat);
        assertNotNull(nat.forFirstType());
        assertNotNull(nat.forType("SearchRequest"));

        List<String> all = nat.getMessageNames();
        assertEquals(1, all.size());
        assertEquals("SearchRequest", all.get(0));
        ProtobufMessage msg = schema.getRootType();
        assertEquals(4, msg.getFieldCount());
    }

    public void testBoxAndPoint() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_BOX);
        assertNotNull(schema);
        List<String> all = schema.getMessageTypes();
        assertEquals(2, all.size());
        assertTrue(all.contains("Box"));
        assertTrue(all.contains("Point"));
    }

    public void testRecursive() throws Exception
    {
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(PROTOC_NODE);
        assertNotNull(schema);
        List<String> all = schema.getMessageTypes();
        assertEquals(1, all.size());
        assertEquals("Node", all.get(0));
        ProtobufMessage msg = schema.getRootType();
        assertEquals(3, msg.getFieldCount());
        ProtobufField f = msg.field("id");
        assertNotNull(f);
        assertEquals("id", f.name);
//        List<ProtobufField> fields = msg.get
    }
}
