package com.fasterxml.jackson.dataformat.protobuf.schema;

/**
 * Set of distinct types parsed from protoc, as unified considering
 * that Java makes no distinction between signed and unsigned types.
 */
public enum FieldType
{
    DOUBLE,
    FLOAT,
    VINT, // variable length, signed and unsigned
    INT32, // fixed length, signed and unsigned
    INT64, // fixed length, signed and unsigned
    BOOLEAN,
    STRING,
    BYTES, // byte array
    ENUM, // encoded as vint
    MESSAGE // object
    ;
}
