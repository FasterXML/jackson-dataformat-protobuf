package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;

/**
 * Simple container for Schemas that have already been generated during
 * generation process; used to share definitions.
 */
public class DefinedProtobufSchemas
{
    protected final Map<JavaType, NativeProtobufSchema> _schemas = new LinkedHashMap<JavaType, NativeProtobufSchema>();

    protected SerializerProvider _provider;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */
    
    public DefinedProtobufSchemas() { }

    public void setProvider(SerializerProvider provider) {
        _provider = provider;
    }
    
    public SerializerProvider getProvider() {
        return _provider;
    }

    /*
    /**********************************************************************
    /* API
    /**********************************************************************
     */
    
    public NativeProtobufSchema findSchema(JavaType type) {
        return _schemas.get(type);
    }

    public void addSchema(JavaType type, NativeProtobufSchema schema) {
    	NativeProtobufSchema old = _schemas.put(type, schema);
        if (old != null) {
            throw new IllegalStateException("Trying to re-define schema for type "+type);
        }
    }
}
