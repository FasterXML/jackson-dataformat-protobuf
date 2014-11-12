package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.SerializableString;
import com.squareup.protoparser.MessageType.Field;

public class ProtobufField
{
    /**
     * Combination of numeric tag and 3-bit wire type.
     */
    public final int typedTag;
    
    /**
     * Name of field in protoc definition
     */
    public final String name;

    public final FieldType type;

    /**
     * 3-bit id used on determining details of how values are serialized.
     */
    public final int wireType;
    
    public final boolean required, repeated, packed, deprecated;
    public final boolean usesZigZag;

    /**
     * For main type of {@link FieldType#MESSAGE}, reference to actual
     * message type definition.
     */
    protected ProtobufMessage messageType;

    /**
     * For fields of type {@link FieldType#ENUM}, mapping from names to ids.
     */
    protected final Map<String,Integer> enumValues;

    protected final boolean isObject;
    
    public ProtobufField(Field nativeField, FieldType type) {
        this(nativeField, type, null, null);
    }

    public ProtobufField(Field nativeField, ProtobufMessage msg) {
        this(nativeField, FieldType.MESSAGE, msg, null);
    }

    public ProtobufField(Field nativeField, ProtobufEnum et) {
        this(nativeField, FieldType.ENUM, null, et);
    }

    public static ProtobufField unknownField() {
        return new ProtobufField(null, FieldType.MESSAGE, null, null);
    }
    
    protected ProtobufField(Field nativeField, FieldType type,
            ProtobufMessage msg, ProtobufEnum et)
    {
        this.type = type;
        wireType = type.getWireType();
        usesZigZag = type.usesZigZag();
        if (et == null) {
            enumValues = Collections.emptyMap();
        } else {
            enumValues = et.valueMapping();
        }
        messageType = msg;

        if (nativeField == null) { // for "unknown" field
            typedTag = 0;
            repeated = required = deprecated = packed = false;
            name = "UNKNOWN";
        } else {
            typedTag = (nativeField.getTag() << 3) + wireType;
            name = nativeField.getName();
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
        isObject = (type == FieldType.MESSAGE);
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

    public int findEnumIndex(SerializableString key) {
        // !!! TODO: optimize if possible
        Integer I = enumValues.get(key.getValue());
        return (I == null) ? -1 : I.intValue();
    }

    public int findEnumIndex(String key) {
        Integer I = enumValues.get(key);
        return (I == null) ? -1 : I.intValue();
    }

    public Collection<String> getEnumValues() {
        return enumValues.keySet();
    }

    public boolean isObject() {
        return isObject;
    }

    public boolean isArray() {
        return repeated;
    }

    @Override
    public String toString() // for debugging
    {
        return "Field '"+name+"', tag="+typedTag+", wireType="+wireType+", fieldType="+type;
    }
}
