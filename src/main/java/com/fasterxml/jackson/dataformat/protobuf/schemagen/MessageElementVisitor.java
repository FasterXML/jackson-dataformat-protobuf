package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.squareup.protoparser.DataType;
import com.squareup.protoparser.DataType.NamedType;
import com.squareup.protoparser.DataType.ScalarType;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.FieldElement.Label;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.TypeElement;

public class MessageElementVisitor extends JsonObjectFormatVisitor.Base implements TypeElementBuilder {

	MessageElement.Builder _builder;

	int _tagCounter = 1;

	Set<TypeElement> _nestedTypes = new HashSet<>();

	JavaType _type;

	public MessageElementVisitor(SerializerProvider provider, JavaType type) {
		super(provider);
		_type = type;
		_builder = MessageElement.builder();
		_builder.name(type.getRawClass().getSimpleName());
		_builder.documentation("Message for " + type.toCanonical());
	}

	@Override
	public TypeElement build() {
		_builder.addTypes(_nestedTypes);
		return _builder.build();
	}

	@Override
	public void property(BeanProperty writer) throws JsonMappingException {
		FieldElement fElement = buildFieldElement(writer, Label.REQUIRED);
		_builder.addField(fElement);
	}

	@Override
	public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
	}

	@Override
	public void optionalProperty(BeanProperty writer) throws JsonMappingException {
		FieldElement fElement = buildFieldElement(writer, Label.OPTIONAL);
		_builder.addField(fElement);
	}

	protected FieldElement buildFieldElement(BeanProperty writer, Label label) throws JsonMappingException {
		FieldElement.Builder fBuilder = FieldElement.builder();

		fBuilder.name(writer.getName());
		fBuilder.tag(nextTag()); // TODO: use tag annotation for indexes.

		JavaType type = writer.getType();

		if (type.isArrayType() || type.isCollectionLikeType()) {
			fBuilder.label(Label.REPEATED);
			fBuilder.type(getDataType(type.getContentType()));
		} else {
			fBuilder.label(label);
			fBuilder.type(getDataType(type));
		}
		return fBuilder.build();
	}

	protected int nextTag() {
		return _tagCounter++;
	}

	@Override
	public void optionalProperty(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
	}

	protected DataType getDataType(JavaType type) throws JsonMappingException {

		ScalarType sType = ProtobuffSchemaHelper.getScalarType(type);
		if (sType != null)
			return sType;

		if (_type != type) { //No self ref
			if (Arrays.asList(_type.getRawClass().getDeclaredClasses()).contains(type.getRawClass())) { //nested class
				TypeElement nestedType = getTypeElement(type);
				_nestedTypes.add(nestedType);
			} else {
				throw new UnsupportedOperationException(
						"Non static nested classes, like \"" + type + "\" are unsupported");
			}
		}

		return NamedType.create(type.getRawClass().getSimpleName());
		//
		// new UnsupportedOperationException(
		// "Protobuf datatype mapping for " + type.getTypeName() + " is not
		// supported (yet)");
	}

	protected TypeElement getTypeElement(JavaType type) throws JsonMappingException {
		SerializerProvider provider = getProvider();
		JsonSerializer<Object> serializer = provider.findValueSerializer(type, null);

		if (type.isEnumType()) {
			throw new UnsupportedOperationException("enums are not supported (yet)");
		}

		RootMessageVisitor visitor = new RootMessageVisitor(provider);
		serializer.acceptJsonFormatVisitor(visitor, type);
		return visitor.builtElement();
	}
}
