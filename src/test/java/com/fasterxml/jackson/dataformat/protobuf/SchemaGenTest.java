package com.fasterxml.jackson.dataformat.protobuf;

import static org.junit.Assert.assertArrayEquals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import com.fasterxml.jackson.dataformat.protobuf.schemagen.ProtobufSchemaGenerator;

import junit.framework.TestCase;

public class SchemaGenTest extends TestCase {

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

	static class WithRequireField {
		@JsonProperty(required = true)
		public byte[] fixedField;

		@JsonProperty(required = true)
		public WrappedByteArray wrappedFixedField;

		void setValue(byte[] bytes) {
			this.fixedField = bytes;
		}

		static class WrappedByteArray {
			@JsonValue
			public ByteBuffer getBytes() {
				return null;
			}
		}
	}

	public void testSimpleObjGenProtobufSchemaTest() throws Exception {
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

	public void testComplexObjGenProtobufSchemaTest() throws Exception {
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

	public void testWithRequireFieldGenProtobufSchemaTest() throws Exception {
		ObjectMapper mapper = new ProtobufMapper();
		ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
		mapper.acceptJsonFormatVisitor(WithRequireField.class, gen);
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
}
