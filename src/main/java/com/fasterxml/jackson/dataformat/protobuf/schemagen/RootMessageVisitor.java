package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNullFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.squareup.protoparser.TypeElement;

public class RootMessageVisitor
    extends JsonFormatVisitorWrapper.Base
{
	
	protected DefinedProtobufSchemas _definedSchemas;
    
	protected TypeElementBuilder _builder;
    
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public RootMessageVisitor(SerializerProvider p) {
        super(p);
    }
    
    public RootMessageVisitor(SerializerProvider p, DefinedProtobufSchemas definedSchemas) {
    	super(p);
    	_definedSchemas = (definedSchemas == null) ? new DefinedProtobufSchemas() : definedSchemas;
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public TypeElement builtElement() {
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
    	MessageElementVisitor visitor = new MessageElementVisitor(_provider, type);
        _builder = visitor;
        return visitor;
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
