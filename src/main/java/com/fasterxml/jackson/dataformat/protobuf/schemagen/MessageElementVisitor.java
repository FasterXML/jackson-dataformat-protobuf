package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
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

	TagGenerator _tagGenerator;

	HashMap<JavaType, TypeElement> _nestedTypes = new HashMap<JavaType, TypeElement>();

	JavaType _type;

	HashSet<JavaType> _dependencies;

	public MessageElementVisitor(SerializerProvider provider, JavaType type) {
		super(provider);

		_type = type;

		_dependencies = new HashSet<JavaType>();

		_builder = MessageElement.builder();
		_builder.name(type.getRawClass().getSimpleName());
		_builder.documentation("Message for " + type.toCanonical());
	}

	@Override
	public TypeElement build() {
		_builder.addTypes(_nestedTypes.values());
		return _builder.build();
	}

	@Override
	public HashSet<JavaType> dependencies() {
		return _dependencies;
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

	@Override
	public void optionalProperty(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
	}

	protected FieldElement buildFieldElement(BeanProperty writer, Label label) throws JsonMappingException {
		FieldElement.Builder fBuilder = FieldElement.builder();

		fBuilder.name(writer.getName());

		fBuilder.tag(nextTag(writer));

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

	protected int nextTag(BeanProperty writer) {
		getTagGenerator(writer);
		return _tagGenerator.nextTag(writer);
	}

	protected void getTagGenerator(BeanProperty writer) {
		if (_tagGenerator == null) {
			if (ProtobuffSchemaHelper.hasIndexAnnotation(writer)) {
				_tagGenerator = new AnnotationBasedTagGenerator();
			} else {
				_tagGenerator = new DefaultTagGenerator();
			}
		}
	}

	protected DataType getDataType(JavaType type) throws JsonMappingException {
		ScalarType sType = ProtobuffSchemaHelper.getScalarType(type);
		if (sType != null)
			return sType;

		if (_type != type) { // No self ref
			if (Arrays.asList(_type.getRawClass().getDeclaredClasses()).contains(type.getRawClass())) { // nested
																										// class
				if (!_nestedTypes.containsKey(type)) { // create nested type
					TypeElement nestedType = getTypeElement(type);
					_nestedTypes.put(type, nestedType);
				}
			} else { // tracking non-nested types to generate them later
				_dependencies.add(type);
			}
		}
		return NamedType.create(type.getRawClass().getSimpleName());
	}

	protected TypeElement getTypeElement(JavaType type) throws JsonMappingException {
		TypeElementBuilder builder = ProtobuffSchemaHelper.acceptTypeElement(_provider, type);
		_dependencies.addAll(builder.dependencies());
		return builder.build();
	}
}
