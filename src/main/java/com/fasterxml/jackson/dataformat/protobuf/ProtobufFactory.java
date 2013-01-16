package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.*;

public class ProtobufFactory extends JsonFactory
{
    private static final long serialVersionUID = 1;

    public final static String FORMAT_NAME_AVRO = "avro";

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */
    
    public ProtobufFactory() { }

    public ProtobufFactory(ObjectCodec codec) {
        super(oc);
    }

    /*                                                                                       
    /**********************************************************                              
    /* Versioned                                                                             
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }
}
