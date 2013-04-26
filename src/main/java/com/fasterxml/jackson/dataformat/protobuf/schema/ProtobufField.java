package com.fasterxml.jackson.dataformat.protobuf.schema;

public class ProtobufField
{
    /**
     * Name of field in protoc definition
     */
    public final String _name;
    
    /**
     * Numeric id used in protobuf message
     */
    public final int _id;

    public ProtobufField(String name, int id)
    {
        _name = name;
        _id = id;
    }
}
