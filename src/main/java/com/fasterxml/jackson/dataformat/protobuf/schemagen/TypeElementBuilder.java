package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.squareup.protoparser.TypeElement;

public interface TypeElementBuilder {

	TypeElement build();

	Set<JavaType> dependencies();
}
