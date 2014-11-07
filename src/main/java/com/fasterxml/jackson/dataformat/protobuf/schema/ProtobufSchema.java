package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.List;

import com.fasterxml.jackson.core.FormatSchema;

/**
 * A {@link FormatSchema} implementation for protobuf, bound to specific root-level
 * {@link ProtobufMessage}, and useful for reading/writing protobuf content
 * that encodes instance of that message.
 */
public class ProtobufSchema implements FormatSchema
{
    public final static String FORMAT_NAME_PROTOBUF = "protobuf";

    /**
     * In case we want to use a different root type, we'll also hold
     * a reference to the native definition.
     */
    protected final NativeProtobufSchema _source;
    
    protected final ProtobufMessage _rootType;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    public ProtobufSchema(NativeProtobufSchema src, ProtobufMessage rootType) {
        _source = src;
        _rootType = rootType;
    }

    /**
     * Method that can be called to choose different root type (of types
     * defined in protoc); a new schema instance will be constructed
     * if type is different from current root type.
     *<p>
     * Note that cost of changing root type is non-trivial in that traversal
     * of types defined is needed -- but exact cost depends on number of types
     * defined. Since schema instances are immutable, it makes sense to try to
     * reuse instances if possible.
     * 
     * @throws IllegalArgumentException If no type with specified name is found
     *   from within this schema.
     */
    public ProtobufSchema withRootType(String typeName)
        throws IllegalArgumentException
    {
        if (_rootType.getName().equals(typeName)) {
            return this;
        }
        return _source.forType(typeName);
    }
    
    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    public NativeProtobufSchema getSource() {
        return _source;
    }

    public ProtobufMessage getRootType() {
        return _rootType;
    }

    /**
     * Accessor for listing names of all root-level messages defined in the
     * original protoc.
     */
    public List<String> getMessageTypes() {
        return _source.getMessageNames();
    }

    @Override
    public String getSchemaType() {
        return FORMAT_NAME_PROTOBUF;
    }
}
