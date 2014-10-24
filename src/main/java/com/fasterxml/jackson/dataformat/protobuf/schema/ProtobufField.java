package com.fasterxml.jackson.dataformat.protobuf.schema;

import com.squareup.protoparser.MessageType.Field;

public class ProtobufField
{
    /**
     * Numeric id ("tag") used in protobuf message
     */
    public final int id;
    
    /**
     * Name of field in protoc definition
     */
    public final String name;

    public final FieldType type;

    public final boolean required, repeated, packed, deprecated;
    
    /**
     * For main type of {@link FieldType#MESSAGE}, reference to actual
     * message type definition.
     */
    protected ProtobufMessage messageType;

    protected final ProtobufEnum enumType;

    public ProtobufField(Field nativeField, FieldType type) {
        this(nativeField, type, null, null);
    }

    public ProtobufField(Field nativeField, ProtobufMessage msg) {
        this(nativeField, FieldType.MESSAGE, msg, null);
    }

    public ProtobufField(Field nativeField, ProtobufEnum et) {
        this(nativeField, FieldType.ENUM, null, et);
    }
    
    protected ProtobufField(Field nativeField, FieldType type,
            ProtobufMessage msg, ProtobufEnum et)
    {
        this.type = type;
        this.id = nativeField.getTag();
        name = nativeField.getName();
        enumType = et;
        messageType = msg;
        switch (nativeField.getLabel()) {
        case REPEATED:
            required = false;
            repeated = true;
            break;
        case REQUIRED:
            required = true;
            repeated = false;
            break;
        default:
            required = repeated = false;
            break;
        }
        packed = nativeField.isPacked();
        deprecated = nativeField.isDeprecated();
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

    public ProtobufEnum getEnumType() {
        return enumType;
    }
}

