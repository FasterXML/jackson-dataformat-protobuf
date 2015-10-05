package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.squareup.protoparser.DataType;
import com.squareup.protoparser.DataType.ScalarType;

public class ProtobuffSchemaHelper {
	
	private ProtobuffSchemaHelper(){}
	
	public static String getNamespace(JavaType type) {
		Class<?> cls = type.getRawClass();
		Package pkg = cls.getPackage();
		return (pkg == null) ? "" : pkg.getName();
	}

	public static ScalarType getScalarType(JavaType type) {
		if (type.hasRawClass(int.class)) {
			return DataType.ScalarType.INT32;
		} else if (type.hasRawClass(long.class) || type.hasRawClass(BigInteger.class)) {
			return DataType.ScalarType.INT64;
		} else if (type.hasRawClass(String.class)) {
			return DataType.ScalarType.STRING;
		} else if (type.hasRawClass(float.class)) {
			return DataType.ScalarType.FLOAT;
		} else if (type.hasRawClass(boolean.class)) {
			return DataType.ScalarType.BOOL;
		} else if (type.hasRawClass(byte.class) || type.hasRawClass(ByteBuffer.class)) {
			return DataType.ScalarType.BYTES;
		} else if (type.hasRawClass(double.class) || type.hasRawClass(BigDecimal.class)) {
			return DataType.ScalarType.DOUBLE;
		}
		return null;
	}
	
	public static TypeElementBuilder acceptTypeElement(SerializerProvider provider, JavaType javaType) throws JsonMappingException {
		JsonSerializer<Object> serializer = provider.findValueSerializer(javaType, null);
		ProtoBufSchemaVisitor visitor = new ProtoBufSchemaVisitor(provider);
		serializer.acceptJsonFormatVisitor(visitor, javaType);
		return visitor;
	}
}
