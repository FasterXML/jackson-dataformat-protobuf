package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.Arrays;

/**
 * Set of distinct types parsed from protoc, as unified considering
 * that Java makes no distinction between signed and unsigned types.
 */
public enum FieldType
{
    DOUBLE("double"), // fixed-length 64-bit double
    FLOAT("float"), // fixed-length
    VINT32("int32", "uint32", "sint32"), // variable length, intended as 32-bit
    VINT64("int64", "uint64", "sint64"), // variable length, intended as 64-bit
    INT32("fixed32", "sfixed32"), // fixed length, 32-bit int
    INT64("fixed64", "sfixed64"), // fixed length, 64-bit int
    BOOLEAN("bool)"),
    STRING("string"),
    BYTES("bytes"), // byte array
    ENUM, // encoded as vint
    MESSAGE // object
    ;

    private final String[] _aliases;

    private FieldType(String... aliases) {
        _aliases = aliases;
    }

    public Iterable<String> aliases() {
        return Arrays.asList(_aliases);
    }
}
