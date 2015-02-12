package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.fasterxml.jackson.core.SerializableString;

public class ProtobufMessage
{
    private final static ProtobufField[] NO_FIELDS = new ProtobufField[0];
    
    protected final String _name;

    /**
     *<p>
     * NOTE: although final, entries are added straight into Map, after
     * constructor has finished. Same is true for <code>_fields</code>
     */
    protected final Map<String,ProtobufField> _fieldsByName;

    protected final ProtobufField[] _fields;

    public ProtobufMessage(String name, Map<String,ProtobufField> fieldsByName,
            ProtobufField[] fields)
    {
        _name = name;
        _fieldsByName = fieldsByName;
        _fields = fields;
    }

    public static ProtobufMessage bogusMessage(String desc) {
        return new ProtobufMessage(desc, Collections.<String,ProtobufField>emptyMap(), NO_FIELDS);
    }

    public int getFieldCount() { return _fields.length; }

    public String getName() { return _name; }

    public ProtobufField field(String name) {
        return _fieldsByName.get(name);
    }

    // !!! TODO: optimize?
    public ProtobufField field(int id) {
        for (int i = 0, len = _fields.length; i < len; ++i) {
            ProtobufField f = _fields[i];
            if (f.getId() == id) {
                return f;
            }
        }
        // That's ok; caller may mind
        return null;
    }

    // !!! TODO: optimize?
    public ProtobufField field(SerializableString name) {
        return _fieldsByName.get(name.getValue());
    }
    
    public String fieldsAsString() {
        return _fieldsByName.keySet().toString();
    }

    public Iterable<ProtobufField> fields() {
        return _fieldsByName.values();
    }
}
