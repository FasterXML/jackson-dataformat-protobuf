package com.fasterxml.jackson.dataformat.protobuf.schema;

import com.fasterxml.jackson.core.FormatSchema;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;

public class ProtobufSchema implements FormatSchema
{
    public final static String FORMAT_NAME_PROTOBUF = "protobuf";

    protected ProtobufSchema() { }
    
    public static ProtobufSchema construct(ProtoFile nativeSchema, MessageType rootType)
    {
        // !!! TODO
        return new ProtobufSchema();
    }
    
    @Override
    public String getSchemaType() {
        return FORMAT_NAME_PROTOBUF;
    }
}
