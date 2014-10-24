package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.fasterxml.jackson.core.FormatSchema;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;

public class ProtobufSchema implements FormatSchema
{
    public final static String FORMAT_NAME_PROTOBUF = "protobuf";

    protected ProtobufSchema() {
    }

    public static ProtobufSchema construct(ProtoFile nativeSchema, MessageType rootType,
            Map<String,MessageType> nativeMessageTypes,            
            Map<String,ProtobufEnum> enums)
    {
        Map<String,ProtobufMessage> messageTypes = new HashMap<String,ProtobufMessage>();

        /*
        ProtobufMessage msg = _buildMessage(nativeSchema, )
        */
        
        // !!! TODO
        return new ProtobufSchema();
        
    }
    
    @Override
    public String getSchemaType() {
        return FORMAT_NAME_PROTOBUF;
    }
}
