package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

public class ProtobufMessage
{
    protected final String _name;

    protected final Map<String,ProtobufField> _fields;
    
    public ProtobufMessage(String name, Map<String,ProtobufField> fields)
    {
        _name = name;
        _fields = fields;
    }

    public static ProtobufMessage bogusMessage(String desc) {
        return new ProtobufMessage(desc, Collections.<String,ProtobufField>emptyMap());
    }

    public int getFieldCount() { return _fields.size(); }
    
    public String getName() { return _name; }
    
    public ProtobufField field(String name) {
        return _fields.get(name);
    }

    public String fieldsAsString() {
        return _fields.keySet().toString();
    }
}
