package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.BeanProperty;

public class AnnotationBasedTagGenerator implements TagGenerator {

	@Override
	public int nextTag(BeanProperty writer) {
		if (ProtobuffSchemaHelper.hasIndexAnnotation(writer)) {
			return ProtobuffSchemaHelper.getJsonProperty(writer).index();
		}
		throw new IllegalStateException("No 'JsonProperty.index' annotation found for " + writer.getFullName()
				+ ", either annotate all properties of type " + writer.getWrapperName().getSimpleName() + " with indexes or none at all");
	}

}
