package com.fasterxml.jackson.dataformat.protobuf;

import com.squareup.proto.ProtoFile;

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
    public ProtobufSchema forType(String messageType)
    {
        return null;
    }

    /**
     * Factory method for constructing Jackson-digestible schema using the first
     * Message type defined in the underlying native protobuf schema.
     */
    public ProtobufSchema forFirstType()
    {
        return null;
    }
}
