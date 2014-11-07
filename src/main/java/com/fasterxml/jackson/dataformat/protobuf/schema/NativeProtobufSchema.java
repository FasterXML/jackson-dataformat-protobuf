package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.squareup.protoparser.*;

/**
 * Helper class used for wrapping a "raw" protobuf schema; and used
 * as input for creating specific {@link ProtobufSchema} to use for
 * reading/writing protobuf encoded content
 */
public class NativeProtobufSchema
{
    protected final String _name;
    protected final List<Type> _nativeTypes;

    protected volatile String[] _messageNames;
    
    protected NativeProtobufSchema(ProtoFile input)
    {
        _name = input.getFileName();
        _nativeTypes = input.getTypes();
    }

    public static NativeProtobufSchema construct(ProtoFile input) {
        return new NativeProtobufSchema(input);
    }
    
    /**
     * Method for checking whether specified message type is defined by
     * the native schema
     */
    public boolean hasMessageType(String messageTypeName)
    {
        for (Type type : _nativeTypes) {
            if (messageTypeName.equals(type.getName())) {
                if (type instanceof MessageType) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Factory method for constructing Jackson-digestible schema using specified Message type
     * from native protobuf schema.
     */
    public ProtobufSchema forType(String messageTypeName)
    {
        MessageType msg = _messageType(messageTypeName);
        if (msg == null) {
            throw new IllegalArgumentException("Protobuf schema definition (name '"+_name
                    +"') has no message type with name '"+messageTypeName+"': known types: "
                    +getMessageNames());
        }
        return new ProtobufSchema(this, TypeResolver.construct(_nativeTypes).resolve(msg));
    }

    /**
     * Factory method for constructing Jackson-digestible schema using the first
     * Message type defined in the underlying native protobuf schema.
     */
    public ProtobufSchema forFirstType()
    {
        MessageType msg = _firstMessageType();
        if (msg == null) {
            throw new IllegalArgumentException("Protobuf schema definition (name '"+_name
                    +"') contains no message type definitions");
        }
        return new ProtobufSchema(this, TypeResolver.construct(_nativeTypes).resolve(msg));
    }

    public List<String> getMessageNames() {
        if (_messageNames == null) {
            _messageNames = _getMessageNames();
        }
        return Arrays.asList(_messageNames);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected MessageType _firstMessageType() {
        for (Type type : _nativeTypes) {
            if (type instanceof MessageType) {
                return (MessageType) type;
            }
        }
        return null;
    }

    protected MessageType _messageType(String name) {
        for (Type type : _nativeTypes) {
            if ((type instanceof MessageType)
                    && name.equals(type.getName())) {
                return (MessageType) type;
            }
        }
        return null;
    }

    private String[] _getMessageNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (Type type : _nativeTypes) {
            if (type instanceof MessageType) {
                names.add(type.getName());
            }
        }
        return names.toArray(new String[names.size()]);
    }
}
