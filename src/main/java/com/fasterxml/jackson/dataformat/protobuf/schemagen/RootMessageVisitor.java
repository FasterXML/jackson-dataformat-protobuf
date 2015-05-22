package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.squareup.protoparser.MessageElement;

public abstract class RootMessageVisitor
    implements JsonFormatVisitorWrapper
{
    protected SerializerProvider _provider;

    protected MessageElement.Builder _builder;
    
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public RootMessageVisitor(SerializerProvider p) {
        _provider = p;
        _builder = MessageElement.builder();
    }
    
    @Override
    public SerializerProvider getProvider() {
        return _provider;
    }

    @Override
    public void setProvider(SerializerProvider provider) {
//        _schemas.setProvider(provider);
        _provider = provider;
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public MessageElement builtElement() {
        return _builder.build();
    }

    /*
    /**********************************************************************
    /* Callbacks
    /**********************************************************************
     */

    @Override
    public JsonObjectFormatVisitor expectObjectFormat(JavaType type)
    {
        /*
        Schema s = _schemas.findSchema(type);
        if (s != null) {
            _valueSchema = s;
            return null;
        }
        RecordVisitor v = new RecordVisitor(_provider, type, _schemas);
        _builder = v;
        return v;
        
        */
        return null;
    }

    @Override
    public JsonMapFormatVisitor expectMapFormat(JavaType mapType) {
        return _throwUnsupported("'Map' type not supported as root type by protobuf");
    }
    
    @Override
    public JsonArrayFormatVisitor expectArrayFormat(JavaType convertedType) {
        return _throwUnsupported("'Array' type not supported as root type by protobuf");
    }

    @Override
    public JsonStringFormatVisitor expectStringFormat(JavaType type) {
        return _throwUnsupported("'String' type not supported as root type by protobuf");
    }

    @Override
    public JsonNumberFormatVisitor expectNumberFormat(JavaType convertedType) {
        return _throwUnsupported("'Number' type not supported as root type by protobuf");
    }

    @Override
    public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
        return _throwUnsupported("'Integer' type not supported as root type by protobuf");
    }

    @Override
    public JsonBooleanFormatVisitor expectBooleanFormat(JavaType convertedType) {
        return _throwUnsupported("'Boolean' type not supported as root type by protobuf");
    }

    @Override
    public JsonNullFormatVisitor expectNullFormat(JavaType convertedType) {
        return _throwUnsupported("'Null' type not supported as root type by protobuf");
    }

    @Override
    public JsonAnyFormatVisitor expectAnyFormat(JavaType convertedType) {
        return _throwUnsupported("'Any' type not supported as root type by protobuf");
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected <T> T _throwUnsupported() {
        return _throwUnsupported("Format variation not supported");
    }
    protected <T> T _throwUnsupported(String msg) {
        throw new UnsupportedOperationException(msg);
    }
}
