package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.FormatSchema;
import com.squareup.proto.ProtoFile;

public class ProtobufSchema implements FormatSchema
{
    public final static String FORMAT_NAME_PROTOBUF = "protobuf";

    protected ProtobufSchema() { }
    
    public static ProtobufSchema construct(ProtoFile nativeSchema) {
        return new ProtobufSchema();
    }
    
    @Override
    public String getSchemaType() {
        return FORMAT_NAME_PROTOBUF;
    }
}
