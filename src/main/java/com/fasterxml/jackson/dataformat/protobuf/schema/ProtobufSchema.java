package com.fasterxml.jackson.dataformat.protobuf.schema;

import com.fasterxml.jackson.core.FormatSchema;

/**
 * A {@link FormatSchema} implementation for protobuf, bound to specific root-level
 * {@link ProtobufMessage}, and useful for reading/writing protobuf content
 * that encodes instance of that message.
 */
public class ProtobufSchema implements FormatSchema
{
    public final static String FORMAT_NAME_PROTOBUF = "protobuf";

    protected final ProtobufMessage _rootType;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    public ProtobufSchema(ProtobufMessage rootType) {
        _rootType = rootType;
    }

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */
    
    public ProtobufMessage getRootType() {
        return _rootType;
    }
    
    @Override
    public String getSchemaType() {
        return FORMAT_NAME_PROTOBUF;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */


}
