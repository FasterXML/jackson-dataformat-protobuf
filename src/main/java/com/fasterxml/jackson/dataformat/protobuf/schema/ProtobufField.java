package com.fasterxml.jackson.dataformat.protobuf.schema;

public class ProtobufField
{
    /**
     * Numeric id used in protobuf message
     */
    public final int id;
    
    /**
     * Name of field in protoc definition
     */
    public final String name;

    public final FieldType type;

    public final boolean required;

    public final boolean repeated;

    /**
     * For main type of {@link FieldType#MESSAGE}, reference to actual
     * message type definition.
     */
    protected ProtobufMessage messageType;
    
    public ProtobufField(int id, String n, FieldType ft,
            boolean reqd, boolean reptd)
    {
        this.id = id;
        name = n;
        type = ft;
        required = reqd;
        repeated = reptd;
    }

    public void assignMessageType(ProtobufMessage msgType) {
        if (type != FieldType.MESSAGE) {
            throw new IllegalStateException("Can not assign message type for non-message field '"+name+"'");
        }
        messageType = msgType;
    }

    public ProtobufMessage getMessageType() {
        return messageType;
    }
}

