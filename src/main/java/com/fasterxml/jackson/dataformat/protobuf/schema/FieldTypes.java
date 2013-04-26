package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

public class FieldTypes
{
    private final static FieldTypes instance = new FieldTypes();

    private final HashMap<String, FieldType> _types;

    private final String _descs;
    
    private FieldTypes()
    {
        _types = new HashMap<String, FieldType>();
        for (FieldType type : FieldType.values()) {
            for (String id : type.aliases()) {
                _types.put(id, type);
            }
        }
        _descs = new HashSet<String>(_types.keySet()).toString();
    }
    
    public static FieldType findType(String id) {
        return instance._findType(id);
    }

    private FieldType _findType(String id)
    {
        FieldType type = instance._types.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type '"+id+"'; needs to be one of: "+_descs);
        }
        return type;
    }
}
