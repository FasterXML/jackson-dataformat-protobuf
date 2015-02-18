package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage;

/**
 * Replacement of {@link com.fasterxml.jackson.core.json.JsonReadContext}
 * to support features needed to decode nested Protobuf messages.
 */
public final class ProtobufReadContext
    extends JsonStreamContext
{
    /**
     * Parent context for this context; null for root context.
     */
    protected final ProtobufReadContext _parent;

    /**
     * Type of current context.
     */
    protected ProtobufMessage _messageType;

    /**
     * Offset within input buffer where the message represented
     * by this context (if message context) ends.
     */
    protected int _endOffset;
    
    // // // Location information (minus source reference)

    protected String _currentName;
    
    /*
    /**********************************************************
    /* Simple instance reuse slots
    /**********************************************************
     */

    protected ProtobufReadContext _child = null;

    /*
    /**********************************************************
    /* Instance construction, reuse
    /**********************************************************
     */

    public ProtobufReadContext(ProtobufReadContext parent,
            ProtobufMessage messageType, int type, int endOffset)
    {
        super();
        _parent = parent;
        _messageType = messageType;
        _type = type;
        _endOffset = endOffset;
        _index = -1;
    }

    protected void reset(ProtobufMessage messageType, int type, int endOffset)
    {
        _messageType = messageType;
        _type = type;
        _index = -1;
        _currentName = null;
        _endOffset = endOffset;
    }

    // // // Factory methods

    public static ProtobufReadContext createRootContext() {
        return new ProtobufReadContext(null, null, TYPE_ROOT, Integer.MAX_VALUE);
    }

    public ProtobufReadContext createChildArrayContext()
    {
        ProtobufReadContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new ProtobufReadContext(this, _messageType,
                    TYPE_ARRAY, _endOffset);
        } else {
            ctxt.reset(_messageType, TYPE_ARRAY, _endOffset);
        }
        return ctxt;
    }

    public ProtobufReadContext createChildArrayContext(int endOffset)
    {
        ProtobufReadContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new ProtobufReadContext(this, _messageType,
                    TYPE_ARRAY, 0);
        } else {
            ctxt.reset(_messageType, TYPE_ARRAY, endOffset);
        }
        return ctxt;
    }
    
    public ProtobufReadContext createChildObjectContext(ProtobufMessage messageType, int endOffset)
    {
        ProtobufReadContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new ProtobufReadContext(this, messageType,
                    TYPE_OBJECT, endOffset);
            return ctxt;
        }
        ctxt.reset(messageType, TYPE_OBJECT, endOffset);
        return ctxt;
    }

    /*
    /**********************************************************
    /* Abstract method implementation
    /**********************************************************
     */

    @Override
    public String getCurrentName() { return _currentName; }

    @Override
    public ProtobufReadContext getParent() { return _parent; }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public int getEndOffset() { return _endOffset; }

    public ProtobufMessage getMessageType() { return _messageType; }

    public void setMessageType(ProtobufMessage mt) { _messageType = mt; }
    
    /**
     * @return Location pointing to the point where the context
     *   start marker was found
     */
    public JsonLocation getStartLocation(Object srcRef, long byteOffset) {
        // not much we can tell
        return new JsonLocation(srcRef, byteOffset, -1, -1);
    }

    /*
    /**********************************************************
    /* State changes
    /**********************************************************
     */

    public void setCurrentName(String name) throws JsonProcessingException
    {
        _currentName = name;
    }

    /*
    /**********************************************************
    /* Overridden standard methods
    /**********************************************************
     */

    /**
     * Overridden to provide developer readable "JsonPath" representation
     * of the context.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        switch (_type) {
        case TYPE_ROOT:
            sb.append("/");
            break;
        case TYPE_ARRAY:
            sb.append('[');
            sb.append(getCurrentIndex());
            sb.append(']');
            break;
        case TYPE_OBJECT:
            sb.append('{');
            if (_currentName != null) {
                sb.append('"');
                CharTypes.appendQuoted(sb, _currentName);
                sb.append('"');
            } else {
                sb.append('?');
            }
            sb.append('}');
            break;
        }
        return sb.toString();
    }
}
