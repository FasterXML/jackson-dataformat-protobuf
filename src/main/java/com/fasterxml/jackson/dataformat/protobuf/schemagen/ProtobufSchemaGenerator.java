package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.TypeElement;
import com.squareup.protoparser.ProtoFile.Syntax;

/**
 * Class that can generate a {@link ProtobufSchema} for a given Java POJO, using
 * definitions Jackson would use for serialization. An instance is typically
 * given to
 * {@link com.fasterxml.jackson.databind.ObjectMapper#acceptJsonFormatVisitor}
 * which will invoke necessary callbacks.
 */
public class ProtobufSchemaGenerator extends RootMessageVisitor {
	public ProtobufSchemaGenerator() {
		// NOTE: null is fine here, as provider links itself after construction
		super(null);
	}

	public ProtobufSchema getGeneratedSchema() {
		TypeElement typeElement = this.builtElement();

		ProtoFile protoFile = ProtoFile.builder("generated")
				.syntax(Syntax.PROTO_2)
				.addType(typeElement)
				.build();

		return NativeProtobufSchema.construct(protoFile).forFirstType();
	}
}
