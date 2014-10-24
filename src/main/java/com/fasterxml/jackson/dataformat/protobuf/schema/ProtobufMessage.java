package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

public class ProtobufMessage
{
    protected final String _name;

    protected final Map<String,ProtobufField> _fields;
    
    public ProtobufMessage(String name, List<ProtobufField> fields)
    {
        _name = name;
        _fields = new LinkedHashMap<String,ProtobufField>(fields.size());
        for (ProtobufField f : fields) {
            _fields.put(f.name, f);
        }
    }

    public ProtobufField field(String name) {
        return _fields.get(name);
    }
}
