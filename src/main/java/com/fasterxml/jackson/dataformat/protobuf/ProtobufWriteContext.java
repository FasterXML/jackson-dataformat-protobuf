package com.fasterxml.jackson.dataformat.protobuf;

import com.fasterxml.jackson.core.JsonStreamContext;

public abstract class ProtobufWriteContext
    extends JsonStreamContext
{
    protected final ProtobufWriteContext _parent;
    
    protected final ProtobufGenerator _generator;

    protected final ProtobufSchema _schema;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected ProtobufWriteContext(int type, ProtobufWriteContext parent,
            ProtobufGenerator generator, ProtobufSchema schema)
    {
        super();
        _type = type;
        _parent = parent;
        _generator = generator;
        _schema = schema;
    }
    
    // // // Factory methods

    public static ProtobufWriteContext createRootContext(ProtobufGenerator generator, ProtobufSchema schema) {
//        return new RootContext(generator, schema);
        return null;
    }

    /**
     * Factory method called to get a placeholder context that is only
     * in place until actual schema is handed.
     */
    public static ProtobufWriteContext createNullContext() {
        return null;
//        return NullContext.instance;
    }
    
    public abstract ProtobufWriteContext createChildArrayContext();
    public abstract ProtobufWriteContext createChildObjectContext();
    
    @Override
    public final ProtobufWriteContext getParent() { return _parent; }
    
    @Override
    public String getCurrentName() { return null; }

    /**
     * Method that writer is to call before it writes a field name.
     *
     * @return True for Object (record) context; false for others
     */
    public boolean writeFieldName(String name) { return false; }

    public abstract void writeValue(Object value);

    /*
    public void complete(BinaryEncoder encoder) throws IOException {
        throw new IllegalStateException("Can not be called on "+getClass().getName());
    }
    */
    
    public boolean canClose() { return true; }

    protected abstract void appendDesc(StringBuilder sb);
    
    // // // Overridden standard methods
    
    /**
     * Overridden to provide developer writeable "JsonPath" representation
     * of the context.
     */
    @Override
    public final String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        appendDesc(sb);
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Implementations
    /**********************************************************
     */

    /*
    private final static class NullContext
        extends ProtobufWriteContext
    {
        public final static NullContext instance = new NullContext();
        
        private NullContext() {
            super(TYPE_ROOT, null, null, null);
        }

        @Override
        public final ProtobufWriteContext createChildArrayContext() {
            _reportError();
            return null;
        }
        
        @Override
        public final ProtobufWriteContext createChildObjectContext() {
            _reportError();
            return null;
        }
    
        @Override
        public void writeValue(Object value) {
            _reportError();
        }
        
        @Override
        public void appendDesc(StringBuilder sb) {
            sb.append("?");
        }

        protected void _reportError() {
            throw new IllegalStateException("Can not write Avro output without specifying Schema");
        }
    }
    
    private final static class RootContext
        extends ProtobufWriteContext
    {
     // We need to keep reference to the root value here.
        protected GenericContainer _rootValue;
        
        protected RootContext(AvroGenerator generator, Schema schema) {
            super(TYPE_ROOT, null, generator, schema);
        }
        
        @Override
        public final ProtobufWriteContext createChildArrayContext()
        {
            // verify that root type is array (or compatible)
            switch (_schema.getType()) {
            case ARRAY:
            case UNION: // maybe
                break;
            default:
                throw new IllegalStateException("Can not write START_ARRAY; schema type is "
                        +_schema.getType());
            }
            GenericArray<Object> arr = _createArray(_schema);
            _rootValue = arr;
            return new ArrayWriteContext(this, _generator, arr);
        }
        
        @Override
        public final ProtobufWriteContext createChildObjectContext()
        {
            // verify that root type is record (or compatible)
            switch (_schema.getType()) {
            case RECORD:
            case UNION: // maybe
                break;
            default:
                throw new IllegalStateException("Can not write START_OBJECT; schema type is "
                        +_schema.getType());
            }
            GenericRecord rec = _createRecord(_schema);
            _rootValue = rec;
            return new ObjectWriteContext(this, _generator, rec);
        }

        @Override
        public void writeValue(Object value) {
            throw new IllegalStateException("Can not write values directly in root context, outside of Records/Arrays");
        }

        @Override
        public void complete(BinaryEncoder encoder) throws IOException
        {
            new GenericDatumWriter<GenericContainer>(_schema).write(_rootValue, encoder);
        }

        @Override
        public void appendDesc(StringBuilder sb) {
            sb.append("/");
        }
    }
*/
}
