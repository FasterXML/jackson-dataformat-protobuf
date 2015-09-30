package com.fasterxml.jackson.dataformat.protobuf;

import static org.junit.Assert.assertArrayEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import com.fasterxml.jackson.dataformat.protobuf.schemagen.ProtobufSchemaGenerator;

import junit.framework.TestCase;

public class SchemaGenTest extends TestCase {
	public static class MsgA {
		public int a;
	}

	public static class Employee {
		public String name;
		public int age;
		public String[] emails;
		public Employee boss;
	}

	public void testSimpleObjGenProtobufSchemaTest() throws Exception {
		ObjectMapper mapper = new ProtobufMapper();
		ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
		mapper.acceptJsonFormatVisitor(MsgA.class, gen);
		ProtobufSchema schemaWrapper = gen.getGeneratedSchema();

		assertNotNull(schemaWrapper);

		String protoFile = schemaWrapper.getSource().toSchema();
		System.out.println(protoFile);

		MsgA msgA = new MsgA();
		msgA.a = 100;

		byte[] msg = mapper.writerFor(MsgA.class).with(schemaWrapper).writeValueAsBytes(msgA);
		System.out.println(msg);
		ProtobufSchema schema = ProtobufSchemaLoader.std.parse(protoFile);
		MsgA newMsgA = mapper.readerFor(MsgA.class).with(schema).readValue(msg);

		System.out.println(newMsgA);
		assertEquals(msgA.a, newMsgA.a);
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
}
