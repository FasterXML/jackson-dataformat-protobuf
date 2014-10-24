package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

public class FieldTypes
{
    private final static FieldTypes instance = new FieldTypes();

    private final HashMap<String, FieldType> _types;

    private FieldTypes()
    {
        _types = new HashMap<String, FieldType>();
        // Note: since ENUM and MESSAGE have no aliases, they won't be mapped here
        for (FieldType type : FieldType.values()) {
            for (String id : type.aliases()) {
                _types.put(id, type);
            }
        }
    }
    
    public static FieldType findType(String id) {
        return instance._findType(id);
    }

    private FieldType _findType(String id) {
        return instance._types.get(id);
    }
}
