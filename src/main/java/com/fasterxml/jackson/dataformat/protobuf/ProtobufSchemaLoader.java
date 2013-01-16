package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;
import java.net.URL;

import com.dyuproject.protostuff.parser.Proto;
import com.dyuproject.protostuff.parser.ProtoUtil;

public class ProtobufSchemaLoader
{
    protected final static ProtobufSchemaLoader DEFAULT_INSTANCE = new ProtobufSchemaLoader();
    
    public ProtobufSchemaLoader() { }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */
    
    public ProtobufSchema load(URL url) throws IOException
    {
        Proto proto = new Proto();
        try {
            ProtoUtil.loadFrom(url, proto);
        } catch (Exception e) {
            _throw(e);
        }

        // !!! TODO
        
        return null;
    }

    public ProtobufSchema load(File f) throws IOException
    {
        Proto proto = new Proto(f);
        try {
            ProtoUtil.loadFrom(f, proto);
        } catch (Exception e) {
            _throw(e);
        }

        // !!! TODO
        
        return null;
    }

    public ProtobufSchema load(String schemaAsString) throws IOException
    {
        Proto proto = new Proto();
        // this is crazy but...
        InputStream in = new ByteArrayInputStream(schemaAsString.getBytes("UTF-8"));
        try {
            ProtoUtil.loadFrom(in, proto);
        } catch (Exception e) {
            _throw(e);
        }
        in.close();

        // !!! TODO
        
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    public void _throw(Exception e0) throws IOException
    {
        // First, peel it
        Throwable e = e0;
        while (e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof IOException){ 
            throw (IOException) e;
        }
        throw new IOException(e.getMessage(), e);
    }
}
