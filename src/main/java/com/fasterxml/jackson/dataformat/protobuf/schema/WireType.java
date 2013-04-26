package com.fasterxml.jackson.dataformat.protobuf.schema;

/**
 * Enumeration of wire types that protobuf specification defines
 */
public enum WireType
{
    /**
     * Variable-length, zig-zag encoded number, or boolean
     */
    VINT, // 0

    FIXED_64BIT, // 1

    LENGTH_PREFIXED, // 2
    
    GROUP_START, // (deprecated) 3

    GROUP_END, // (deprecated) 4
    
    FIXED_32BIT // 5
    ;

    public static WireType valueOf(int type)
    {
        switch (type) {
        case 0: return VINT;
        case 1: return FIXED_64BIT;
        case 2: return LENGTH_PREFIXED;
        case 3: return GROUP_START;
        case 4: return GROUP_END;
        case 5: return FIXED_32BIT;
        }
        throw new IllegalArgumentException("Unrecognized wire type: "+type);
    }
}
