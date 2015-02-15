package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.SerializableString;
import com.squareup.protoparser.MessageType.Field;

public class ProtobufField implements Comparable<ProtobufField>
{
    /**
     * Numeric tag, unshifted
     */
    public final int id;

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

    /**
     * For fields of type {@link FieldType#ENUM} with non-standard indexing,
     * mapping back from tag ids to enum names.
     */
    protected final Map<Integer,String> enumsById;
    
    /**
     * Link to next field within message definition; used for efficient traversal.
     * Due to inverse construction order need to be assigned after construction;
     * but functionally immutable.
     */
    public ProtobufField next;
    
    public final boolean isObject;

    public final boolean isStdEnum;
    
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
            isStdEnum = false;
            enumsById = null;
        } else {
            enumValues = et.valueMapping();
            isStdEnum = et.usesStandardIndexing();
            if (isStdEnum) {
                enumsById = null;
            } else {
                LinkedHashMap<Integer,String> byId = new LinkedHashMap<Integer,String>();
                for (Map.Entry<String,Integer> entry : enumValues.entrySet()) {
                    byId.put(entry.getValue(), entry.getKey());
                }
                enumsById = byId;
            }
        }
        messageType = msg;

        if (nativeField == null) { // for "unknown" field
            typedTag = id = 0;
            repeated = required = deprecated = packed = false;
            name = "UNKNOWN";
        } else {
            id = nativeField.getTag();
            typedTag = (id << 3) + wireType;
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

    public void assignNext(ProtobufField n) {
        if (this.next != null) {
            throw new IllegalStateException("Can not overwrite 'next' after being set");
        }
        this.next = n;
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
    public String findEnumByIndex(int index) {
        return enumsById.get(Integer.valueOf(index));
    }

    public Collection<String> getEnumValues() {
        return enumValues.keySet();
    }

    public boolean isArray() {
        return repeated;
    }

    public boolean isValidFor(int typeTag) {
        return (typeTag == type.getWireType());
    }

    @Override
    public String toString() // for debugging
    {
        return "Field '"+name+"', tag="+typedTag+", wireType="+wireType+", fieldType="+type;
    }

    @Override
    public int compareTo(ProtobufField other) {
        return id - other.id;
    }
}
