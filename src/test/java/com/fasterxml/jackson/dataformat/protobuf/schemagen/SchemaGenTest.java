package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufTestBase;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

public class SchemaGenTest extends ProtobufTestBase {

	public static class RootType {
		public String name;

		public int value;

		public List<String> other;
	}

	public static class Employee {
		@JsonProperty(required = true)
		public String name;
		@JsonProperty(required = true)
		public int age;
		public String[] emails;
		public Employee boss;
	}

	public void testSimplePojoGenProtobufSchema() throws Exception {
		ObjectMapper mapper = new ProtobufMapper();
		ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
		mapper.acceptJsonFormatVisitor(RootType.class, gen);
		ProtobufSchema schemaWrapper = gen.getGeneratedSchema();

		assertNotNull(schemaWrapper);

		String protoFile = schemaWrapper.getSource().toSchema();
		System.out.println(protoFile);

		RootType rType = new RootType();
		rType.name = "rTpye";
		rType.value = 100;
		rType.other = new ArrayList<String>();
		rType.other.add("12345");
		rType.other.add("abcdefg");

		byte[] msg = mapper.writerFor(RootType.class).with(schemaWrapper).writeValueAsBytes(rType);
		System.out.println(msg);
		ProtobufSchema schema = ProtobufSchemaLoader.std.parse(protoFile);
		RootType parsedRootType = mapper.readerFor(RootType.class).with(schema).readValue(msg);

		System.out.println(parsedRootType);
		assertEquals(rType.name, parsedRootType.name);
		assertEquals(rType.value, parsedRootType.value);
		assertEquals(rType.other, parsedRootType.other);
	}

	public void testSelfRefPojoGenProtobufSchema() throws Exception {
		ObjectMapper mapper = new ProtobufMapper();
		ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
		mapper.acceptJsonFormatVisitor(Employee.class, gen);
		ProtobufSchema schemaWrapper = gen.getGeneratedSchema();

		assertNotNull(schemaWrapper);

		String protoFile = schemaWrapper.getSource().toSchema();
		System.out.println(protoFile);

		Employee empl = new Employee();
		empl.name = "Bobbee";
		empl.age = 39;
		empl.emails = new String[] { "bob@aol.com", "bobby@gmail.com" };
		empl.boss = null;

		byte[] byteMsg = mapper.writerFor(Employee.class).with(schemaWrapper).writeValueAsBytes(empl);
		System.out.println(byteMsg);
		ProtobufSchema schema = ProtobufSchemaLoader.std.parse(protoFile);
		Employee newEmpl = mapper.readerFor(Employee.class).with(schema).readValue(byteMsg);

		System.out.println(newEmpl);
		assertEquals(empl.name, newEmpl.name);
		assertEquals(empl.age, newEmpl.age);
		assertArrayEquals(empl.emails, newEmpl.emails);
		assertEquals(empl.boss, newEmpl.boss);
	}

	public void testComplexPojoGenProtobufSchema() throws Exception {
		ObjectMapper mapper = new ProtobufMapper();
		ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
		mapper.acceptJsonFormatVisitor(MediaItem.class, gen);
		ProtobufSchema schemaWrapper = gen.getGeneratedSchema();
		assertNotNull(schemaWrapper);

		String protoFile = schemaWrapper.getSource().toSchema();
		System.out.println(protoFile);

		MediaItem mediaItem = MediaItem.buildItem();

		byte[] byteMsg = mapper.writerFor(MediaItem.class).with(schemaWrapper).writeValueAsBytes(mediaItem);
		System.out.println(byteMsg);
		ProtobufSchema schema = ProtobufSchemaLoader.std.parse(protoFile);
		MediaItem deserMediaItem = mapper.readerFor(MediaItem.class).with(schema).readValue(byteMsg);

		System.out.println(deserMediaItem);
		assertEquals(mediaItem, deserMediaItem);
	}
}
