package com.fasterxml.jackson.dataformat.protobuf.schemagen;

/**
 * Class that can generate a {@link ProtobufSchema} for a given Java POJO,
 * using definitions Jackson would use for serialization.
 * An instance is typically given to
 * {@link com.fasterxml.jackson.databind.ObjectMapper#acceptJsonFormatVisitor}
 * which will invoke necessary callbacks.
 */
public abstract class ProtobufSchemaGenerator extends VisitorFormatWrapperImpl
{
    public ProtobufSchemaGenerator() {
        // NOTE: null is fine here, as provider links itself after construction
        super(null);
    }

    /*
    public AvroSchema getGeneratedSchema() {
        return new AvroSchema(getAvroSchema());
    }
    */
}
