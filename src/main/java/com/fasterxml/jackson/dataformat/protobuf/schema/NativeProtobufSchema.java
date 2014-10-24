package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;

/**
 * Helper class used for wrapping a "raw" protobuf schema; and used
 * as input for creating specific {@link ProtobufSchema} to use for
 * reading/writing protobuf encoded content
 */
public class NativeProtobufSchema
{
    protected final ProtoFile _native;

    protected final LinkedHashMap<String,MessageType> _nativeMessageTypes;

    protected final Map<String,ProtobufEnum> _enums;
    
    protected NativeProtobufSchema(ProtoFile input, LinkedHashMap<String,MessageType> nativeMsgs,
            Map<String,ProtobufEnum> enums)
    {
        _native = input;
        _nativeMessageTypes = nativeMsgs;
        _enums = enums;
    }

    public static NativeProtobufSchema construct(ProtoFile input)
    {
        LinkedHashMap<String,MessageType> nativeMessages = new LinkedHashMap<String,MessageType>();
        Map<String,ProtobufEnum> enumTypes = new HashMap<String,ProtobufEnum>();
        
        for (Type nt : input.getTypes()) {
            if (nt instanceof MessageType) {
                nativeMessages.put(nt.getName(), (MessageType) nt);
            } else if (nt instanceof EnumType) {
                enumTypes.put(nt.getName(), _constructEnum((EnumType) nt));
            } // no other known types?
        }
        return new NativeProtobufSchema(input, nativeMessages, enumTypes);
    }

    protected static ProtobufEnum _constructEnum(EnumType nativeEnum)
    {
        final Map<String,Integer> valuesByName = new LinkedHashMap<String,Integer>();
        for (EnumType.Value v : nativeEnum.getValues()) {
            valuesByName.put(v.getName(), v.getTag());
        }
        return new ProtobufEnum(nativeEnum.getName(), valuesByName);
    }
    
    /**
     * Method for checking whether specified message type is defined by
     * the native schema
     */
    public boolean hasMessageType(String messageTypeName)
    {
        for (Type type : _native.getTypes()) {
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
            throw new IllegalArgumentException("Protobuf schema definition (name '"+_native.getFileName()
                    +"') has no message type with name '"+messageTypeName+"'");
        }
        return ProtobufSchema.construct(_native, msg, _nativeMessageTypes, _enums);
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
        return ProtobufSchema.construct(_native, msg, _nativeMessageTypes, _enums);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected MessageType _firstMessageType() {
        Iterator<MessageType> it = _nativeMessageTypes.values().iterator();
        return it.hasNext() ? it.next() : null;
    }

    protected MessageType _messageType(String name) {
        return _nativeMessageTypes.get(name);
    }
}
