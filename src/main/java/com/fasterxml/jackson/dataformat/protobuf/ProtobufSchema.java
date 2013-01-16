package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.FormatSchema;

public class ProtobufSchema implements FormatSchema
{
    public final static String FORMAT_NAME_PROTOBUF = "protobuf";

    @Override
    public String getSchemaType() {
        return FORMAT_NAME_PROTOBUF;
    }
}
