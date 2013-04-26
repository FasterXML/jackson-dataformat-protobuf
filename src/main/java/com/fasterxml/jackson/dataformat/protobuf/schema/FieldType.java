package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.Arrays;

/**
 * Set of distinct types parsed from protoc, as unified considering
 * that Java makes no distinction between signed and unsigned types.
 */
public enum FieldType
{
    DOUBLE("double"),
    FLOAT("float"),
    VINT("int32", "int64"), // variable length, signed and unsigned
    INT32("uint32", "sint32", "fixed32", "sfixed32"), // fixed length, signed and unsigned
    INT64("uint64", "sint64", "fixed64", "sfixed64"), // fixed length, signed and unsigned
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
