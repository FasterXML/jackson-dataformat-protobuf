package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.fasterxml.jackson.core.SerializableString;

public class ProtobufMessage
{
    private final static ProtobufField[] NO_FIELDS = new ProtobufField[0];

    // Let's allow reasonable sized lookup arrays
    private final static int MAX_FIELD_INDEX_SIZE = 200;
    //    private final static int[] NO_INTS = new int[0];

    protected final String _name;

    /**
     *<p>
     * NOTE: although final, entries are added straight into Map, after
     * constructor has finished. Same is true for <code>_fields</code>
     */
    protected final Map<String,ProtobufField> _fieldsByName;

    protected final ProtobufField[] _fields;

    /**
     * Arrays of fields indexed by id (offset by <code>_idOffset</code>), if
     * fields ids are in contiguous (enough) range.
     */
    protected ProtobufField[] _fieldsById;

    protected ProtobufField _firstField;

    protected int _idOffset = -1;
    
    public ProtobufMessage(String name, Map<String,ProtobufField> fieldsByName,
            ProtobufField[] fields)
    {
        _name = name;
        _fieldsByName = fieldsByName;
        _fields = fields;
    }

    /**
     * Method called right after finishing actual construction of this
     * message definition. Needed because assignment to fields is dynamic,
     * and setup is NOT complete when constructor exits.
     */
    public void init(ProtobufField first)
    {
        _firstField = first;

        // Let's see, as well, whether we can create a direct lookup index.
        // Note that fields have been sorted by caller already.
        int len = _fields.length;

        if (len > 0) {
            int firstId = _fields[0].id;
            int lastId = _fields[len-1].id;
            if (firstId > lastId) {
                throw new IllegalStateException("Internal error: first id ("+firstId+") > last id ("
                        +lastId+")");
            }
            int size = lastId - firstId + 1;
            if (size <= MAX_FIELD_INDEX_SIZE) {
                _idOffset = firstId;
                _fieldsById = new ProtobufField[size];
                for (ProtobufField f : _fields) {
                    // another sanity check for fun
                    int index = f.id - _idOffset;
                    if (_fieldsById[index] != null) {
                        throw new IllegalStateException("Internal error: collision for message of type '"
                                +_name+"' for id "+f.id);
                    }
                    _fieldsById[index] = f;
                }
            }
        }
    }

    public static ProtobufMessage bogusMessage(String desc) {
        ProtobufMessage bogus = new ProtobufMessage(desc, Collections.<String,ProtobufField>emptyMap(), NO_FIELDS);
        bogus.init(null);
        return bogus;
    }

    public ProtobufField firstField() { return _firstField; }
    
    public int getFieldCount() { return _fields.length; }

    public String getName() { return _name; }

    public ProtobufField field(String name) {
        return _fieldsByName.get(name);
    }

    // !!! TODO: optimize?
    public ProtobufField field(int id)
    {
        // Can we just index it?
        int idOffset = _idOffset;
        if (idOffset >= 0) {
            return _fieldsById[id - idOffset];
        }
        // if not, brute force works
        for (int i = 0, len = _fields.length; i < len; ++i) {
            ProtobufField f = _fields[i];
            if (f.id == id) {
                return f;
            }
        }
        // not found? that's ok with us, but caller may mind
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
