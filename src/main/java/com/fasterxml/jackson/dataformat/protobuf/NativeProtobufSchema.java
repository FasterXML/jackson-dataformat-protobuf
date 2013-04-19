package com.fasterxml.jackson.dataformat.protobuf;

import com.squareup.proto.MessageType;
import com.squareup.proto.ProtoFile;
import com.squareup.proto.Type;

/**
 * Helper class used for wrapping a "raw" protobuf schema; and used
 * as input for creating specific {@link ProtobufSchema} to use for
 * reading/writing protobuf encoded content
 */
public class NativeProtobufSchema
{
    protected final ProtoFile _native;
    
    protected NativeProtobufSchema(ProtoFile input) {
        _native = input;
    }

    public static NativeProtobufSchema construct(ProtoFile input) {
        return new NativeProtobufSchema(input);
    }

    /**
     * Factory method for constructing Jackson-digestible schema using specified Message type
     * from native protobuf schema.
     */
    public ProtobufSchema forType(String messageTypeName)
    {
        MessageType msg = _messageType(messageTypeName);
        if (msg == null) {
            throw new IllegalArgumentException("Protobuf schema definition (name '"+_native.getFileName()
                    +"') has no message type with name '"+messageTypeName+"'");
        }
        return ProtobufSchema.construct(_native, msg);
    }

    /**
     * Factory method for constructing Jackson-digestible schema using the first
     * Message type defined in the underlying native protobuf schema.
     */
    public ProtobufSchema forFirstType()
    {
        MessageType msg = _firstMessageType();
        if (msg == null) {
            throw new IllegalArgumentException("Protobuf schema definition (name '"+_native.getFileName()
                    +"') contains no message type definitions");
        }
        return ProtobufSchema.construct(_native, msg);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected MessageType _firstMessageType()
    {
        for (Type type : _native.getTypes()) {
            if (type instanceof MessageType) {
                return (MessageType) type;
            }
        }
        return null;
    }

    protected MessageType _messageType(String name)
    {
        for (Type type : _native.getTypes()) {
            if (name.equals(type.getName())) {
                if (type instanceof MessageType) {
                    return (MessageType) type;
                }
            }
        }
        return null;
    }
}
