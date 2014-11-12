package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.Map;

public class ProtobufEnum
{
    protected final String _name;

    protected final Map<String,Integer> _valuesByName;

    public ProtobufEnum(String name, Map<String,Integer> valuesByName)
    {
        _name = name;
        _valuesByName = valuesByName;
    }

    public Integer findEnum(String name) {
        return _valuesByName.get(name);
    }

    public Map<String,Integer> valueMapping() {
        return _valuesByName;
    }
}
