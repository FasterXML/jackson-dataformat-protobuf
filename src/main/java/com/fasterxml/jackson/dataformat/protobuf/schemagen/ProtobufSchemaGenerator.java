package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.squareup.protoparser.TypeElement;

/**
 * Class that can generate a {@link ProtobufSchema} for a given Java POJO, using
 * definitions Jackson would use for serialization. An instance is typically
 * given to
 * {@link com.fasterxml.jackson.databind.ObjectMapper#acceptJsonFormatVisitor}
 * which will invoke necessary callbacks.
 */
public class ProtobufSchemaGenerator extends ProtoBufSchemaVisitor {

	protected HashSet<JavaType> _generated;

	protected JavaType _rootType;

	public ProtobufSchemaGenerator() {
		// NOTE: null is fine here, as provider links itself after construction
		super(null);
	}

	public ProtobufSchema getGeneratedSchema() throws JsonMappingException {
		if (_rootType == null) {
			throw new IllegalStateException(
					"No visit methods called on " + getClass().getName() + ": no schema generated");
		}
		HashMap<JavaType, TypeElement> typeElements = new LinkedHashMap<JavaType, TypeElement>();
		typeElements.put(_rootType, this.build());
		resolveDependencies(this.dependencies(), typeElements);

		return NativeProtobufSchema
				.construct(_rootType.getRawClass().getName(), new ArrayList<TypeElement>(typeElements.values()))
				.forFirstType();
	}

	@Override
	public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
		_rootType = type;
		return super.expectObjectFormat(type);
	}

	@Override
	public JsonStringFormatVisitor expectStringFormat(JavaType type) {
		return _throwUnsupported("'String' type not supported as root type by protobuf");
	}

	protected void resolveDependencies(Set<JavaType> dependencies, HashMap<JavaType, TypeElement> definedElements)
			throws JsonMappingException {
		if (dependencies.isEmpty())
			return;

		Set<JavaType> alsoResolve = new HashSet<JavaType>();

		for (JavaType javaType : dependencies) {
			if (!definedElements.containsKey(javaType)) {
				TypeElementBuilder visitor = ProtobuffSchemaHelper.acceptTypeElement(_provider, javaType);
				if (visitor.dependencies() != null) {
					alsoResolve.addAll(visitor.dependencies());
				}
				definedElements.put(javaType, visitor.build());
			}
		}

		alsoResolve.removeAll(definedElements.values());
		resolveDependencies(alsoResolve, definedElements); // recursive resolve
	}
}
