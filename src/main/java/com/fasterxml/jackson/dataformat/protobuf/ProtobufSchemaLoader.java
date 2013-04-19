package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

import com.squareup.proto.ProtoFile;
import com.squareup.proto.ProtoSchemaParser;

public class ProtobufSchemaLoader
{
    private final static Charset UTF8 = Charset.forName("UTF-8");

    public final static String DEFAULT_SCHEMA_NAME = "NEMO";
    
    protected final static ProtobufSchemaLoader DEFAULT_INSTANCE = new ProtobufSchemaLoader();
    
    public ProtobufSchemaLoader() { }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */
    
    public ProtobufSchema load(URL url) throws IOException {
        return schema(loadNative(url));
    }

    public ProtobufSchema load(File f) throws IOException {
        return schema(loadNative(f));
    }

    /**
     * Method for loading and parsing a protoc definition from given
     * stream, assuming UTF-8 encoding.
     * Note that given {@link InputStream} will be closed before method returns.
     */
    public ProtobufSchema load(InputStream in) throws IOException {
        return schema(loadNative(in, true));
    }

    /**
     * Method for loading and parsing a protoc definition from given
     * stream, assuming UTF-8 encoding.
     * Note that given {@link Reader} will be closed before method returns.
     */
    public ProtobufSchema load(Reader r) throws IOException {
        return schema(loadNative(r, true));
    }

    /**
     * Method for parsing given protoc schema definition, constructing
     * schema object Jackson can use.
     */
    public ProtobufSchema parse(String schemaAsString) throws IOException {
        return parse(schemaAsString, DEFAULT_SCHEMA_NAME);
    }

    /**
     * Method for parsing given protoc schema definition, constructing
     * schema object Jackson can use.
     * 
     * @param schemaName Name of Schema used by protoc parser
     */
    public ProtobufSchema parse(String schemaAsString, String schemaName) throws IOException {
        return schema(loadNative(schemaAsString));
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    public ProtoFile loadNative(File f) throws IOException {
        return ProtoSchemaParser.parse(f);
    }

    public ProtoFile loadNative(URL url) throws IOException {
        return loadNative(url.openStream(), true);
    }

    public ProtoFile loadNative(String schemaAsString) throws IOException {
        return ProtoSchemaParser.parse(DEFAULT_SCHEMA_NAME, schemaAsString);
    }
    
    public ProtoFile loadNative(InputStream in, boolean close) throws IOException {
        return loadNative(new InputStreamReader(in, UTF8), close);
    }
    
    protected ProtoFile loadNative(Reader r, boolean close) throws IOException {
        try {
            return ProtoSchemaParser.parse(DEFAULT_SCHEMA_NAME, _readAll(r));
        } finally {
            if (close) {
                try { r.close(); } catch (IOException e) { }
            }
        }
    }

    protected String _readAll(Reader r) throws IOException
    {
        StringBuilder sb = new StringBuilder(1000);
        char[] buffer = new char[1000];
        int count;
        
        while ((count = r.read(buffer)) > 0) {
            sb.append(buffer, 0, count);
        }
        return sb.toString();
    }
    
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

    protected ProtobufSchema schema(ProtoFile nativeSchema) {
        return new ProtobufSchema(nativeSchema);
    }
}
