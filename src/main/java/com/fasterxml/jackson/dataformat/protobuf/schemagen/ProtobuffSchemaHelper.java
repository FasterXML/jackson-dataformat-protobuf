package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.JavaType;

public class ProtobuffSchemaHelper {
	public static String getNamespace(JavaType type) {
		Class<?> cls = type.getRawClass();
        Package pkg = cls.getPackage();
        return (pkg == null) ? "" : pkg.getName();
	}
}
