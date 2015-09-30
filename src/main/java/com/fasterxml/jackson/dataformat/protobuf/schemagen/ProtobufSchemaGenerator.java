package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
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
	ProtoFile.Builder _builder;
	
	public ProtobufSchemaGenerator() {
		// NOTE: null is fine here, as provider links itself after construction
		super(null);
	}

	public ProtobufSchema getGeneratedSchema() {
		if(_builder == null) {
			throw new IllegalStateException("No visit methods called on "+getClass().getName()
                    +": no schema generated");
		}
		
		TypeElement typeElement = this.builtElement();
		_builder.addType(typeElement);
		ProtoFile protoFile = _builder.build();
		return NativeProtobufSchema.construct(protoFile).forFirstType();
	}
	
	@Override
	public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
		_builder = ProtoFile.builder(type.getRawClass().getName());
		_builder.syntax(Syntax.PROTO_2);
		return super.expectObjectFormat(type);
	}
}
