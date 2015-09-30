package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.squareup.protoparser.DataType;
import com.squareup.protoparser.DataType.NamedType;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.FieldElement.Label;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.TypeElement;

public class MessageElementVisitor extends JsonObjectFormatVisitor.Base implements TypeElementBuilder {

	MessageElement.Builder _builder;
	
	int _tagCounter = 1;

	public MessageElementVisitor(SerializerProvider provider, JavaType type) {
		super(provider);
		_builder = MessageElement.builder();
		_builder.name(type.getRawClass().getSimpleName());
		_builder.documentation("Message for " + type.toCanonical());
	}

	@Override
	public TypeElement build() {
		return _builder.build();
	}

	@Override
	public void property(BeanProperty writer) {
		// TODO Auto-generated method stub
	}

	@Override
	public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
	}

	@Override
	public void optionalProperty(BeanProperty writer) {
		FieldElement.Builder fBuilder = FieldElement.builder();

		fBuilder.name(writer.getName());
		fBuilder.tag(nextTag()); // TODO: use tag annotation for indexes.

		JavaType type = writer.getType();

		fBuilder.type(getDataType(type));

		if (type.isArrayType()) {
			fBuilder.label(Label.REPEATED);
		} else {
			fBuilder.label(Label.OPTIONAL); // TODO: use annotation for labels
		}

		_builder.addField(fBuilder.build());
	}
	
	public int nextTag() {
		return _tagCounter++;
	}

	@Override
	public void optionalProperty(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
	}

	private DataType getDataType(JavaType type) {
		if (type.hasRawClass(int.class)) {
			return DataType.ScalarType.INT32;
		} else if (type.hasRawClass(String.class)) {
			return DataType.ScalarType.STRING;
		} else if (type.isArrayType()) {
			return getDataType(type.getContentType());
		}
		return NamedType.create(type.getRawClass().getSimpleName());
		//
		// new UnsupportedOperationException(
		// "Protobuf datatype mapping for " + type.getTypeName() + " is not
		// supported (yet)");
	}
}
