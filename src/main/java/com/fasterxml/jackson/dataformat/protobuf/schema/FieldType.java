package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.Arrays;

/**
 * Set of distinct types parsed from protoc, as unified considering
 * that Java makes no distinction between signed and unsigned types.
 */
public enum FieldType
{
    DOUBLE(WireType.FIXED_64BIT, "double"), // fixed-length 64-bit double
    FLOAT(WireType.FIXED_32BIT, "float"), // fixed-length, 32-bit single precision
    VINT32_Z(WireType.VINT, "sint32"), // variable length w/ ZigZag, intended as 32-bit
    VINT64_Z(WireType.VINT, "sint64"), // variable length w/ ZigZag, intended as 64-bit
    VINT32_STD(WireType.VINT, "int32", "uint32"), // variable length, intended as 32-bit
    VINT64_STD(WireType.VINT, "int64", "uint64"), // variable length, intended as 64-bit

    FIXINT32(WireType.FIXED_32BIT, "fixed32", "sfixed32"), // fixed length, 32-bit int
    FIXINT64(WireType.FIXED_64BIT, "fixed64", "sfixed64"), // fixed length, 64-bit int
    BOOLEAN(WireType.VINT, "bool)"),
    STRING(WireType.LENGTH_PREFIXED, "string"),
    BYTES(WireType.LENGTH_PREFIXED, "bytes"), // byte array
    ENUM(WireType.VINT), // encoded as vint
    MESSAGE(WireType.LENGTH_PREFIXED) // object
    ;

    private final int _wireType;
    
    private final String[] _aliases;

    private FieldType(int wt, String... aliases) {
        _wireType = wt;
        _aliases = aliases;
    }

    public int getWireType() { return _wireType; }

    public boolean usesZigZag() {
        return (this == VINT32_Z) || (this == VINT64_Z);
    }
    
    public Iterable<String> aliases() {
        return Arrays.asList(_aliases);
    }
}
