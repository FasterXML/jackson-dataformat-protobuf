package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufField;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage;

public class ProtobufWriteContext
    extends JsonStreamContext
{
    protected final ProtobufWriteContext _parent;

    /**
     * Definition of the closest Object that this context relates to;
     * either object for the field (for Message/Object types), or its
     * parent (for Array types)
     */
    protected final ProtobufMessage _message;

    /**
     * Field within either current object (for Object context); or, parent
     * field (for Array)
     */
    protected ProtobufField _field;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected ProtobufWriteContext(int type, ProtobufWriteContext parent,
            ProtobufMessage msg)
    {
        super();
        _type = type;
        _parent = parent;
        _message = msg;
    }
    
    // // // Factory methods

    public static ProtobufWriteContext createRootContext(ProtobufMessage msg) {
        return new ProtobufWriteContext(TYPE_ROOT, null, msg);
    }

    /**
     * Factory method called to get a placeholder context that is only
     * in place until actual schema is handed.
     */
    public static ProtobufWriteContext createNullContext() {
        return null;
    }
    
    public ProtobufWriteContext createChildArrayContext() {
        ProtobufWriteContext ctxt = new ProtobufWriteContext(TYPE_ARRAY, this, _message);
        ctxt._field = _field;
        return ctxt;
    }

    public ProtobufWriteContext createChildObjectContext(ProtobufMessage type) {
        return new ProtobufWriteContext(TYPE_OBJECT, this, type);
    }
    
    @Override
    public final ProtobufWriteContext getParent() { return _parent; }
    
    @Override
    public String getCurrentName() {
        return ((_type == TYPE_OBJECT) && (_field != null)) ? _field.name : null;
    }

    public void setField(ProtobufField f) {
        _field = f;
    }

    public ProtobufField getField() {
        return _field;
    }

    public StringBuilder appendDesc(StringBuilder sb) {
        if (_parent != null) {
            sb = _parent.appendDesc(sb);
        }
        switch (_type) {
        case TYPE_OBJECT:
            if (_field == null) {
                sb.append("{}");
            } else {
                sb.append("{'");
                sb.append(_field.name);
                sb.append("'}");
            }
            break;
        case TYPE_ARRAY:
            sb.append('[');
            sb.append(getCurrentIndex());
            sb.append(']');
            break;
        case TYPE_ROOT:
            // should we do anything?
            sb.append('/');
        }
        return sb;
    }
    
    // // // Overridden standard methods
    
    /**
     * Overridden to provide developer writeable "JsonPath" representation
     * of the context.
     */
    @Override
    public final String toString() {
        return appendDesc(new StringBuilder(64)).toString();
    }
}
