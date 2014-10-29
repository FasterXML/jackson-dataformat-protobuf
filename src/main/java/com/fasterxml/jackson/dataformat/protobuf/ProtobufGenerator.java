package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.dataformat.protobuf.schema.FieldType;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufField;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;

public class ProtobufGenerator extends GeneratorBase
{
    /**
     * Enumeration that defines all togglable features for Protobuf generators
     */
    public enum Feature {
        /**
         * Feature that can be enabled to quietly ignore serialization of unknown
         * fields.
         *<p>
         * Feature is disabled by default, so that an exception is thrown if unknown
         * fields are encountered.
         */
        IGNORE_UNKNOWN_FIELDS(false)
        ;

        protected final boolean _defaultState;
        protected final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }

        public boolean enabledIn(int flags) { return (flags & getMask()) != 0; }
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    };

    /*
    /**********************************************************
    /* Constants
    /**********************************************************
     */

    /**
     * Since our context object does NOT implement standard write context, need
     * to do something like use a placeholder...
     */
    protected final static JsonWriteContext BOGUS_WRITE_CONTEXT = JsonWriteContext.createRootContext(null);
    
    /**
     * This instance is used as a placeholder for cases where we do not know
     * actual field and want to simply skip over any values that caller tries
     * to write for it.
     */
    protected final static ProtobufField UNKNOWN_FIELD = ProtobufField.unknownField();

    /**
     * This is used as a placeholder for case where we don't have an actual message
     * to use, but know (from context) that one is expected.
     */
    protected final static ProtobufMessage UNKNOWN_MESSAGE = ProtobufMessage.bogusMessage("<unknown>");

    protected final static ProtobufMessage ROOT_MESSAGE = ProtobufMessage.bogusMessage("<root>");
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link ProtobufGenerator.Feature}s
     * are enabled.
     */
    protected int _protobufFeatures;

    protected ProtobufSchema _schema;

    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * Reference to the root context since that is needed for serialization
     */
    protected ProtobufWriteContext _rootContext;

    protected boolean _inObject;

    /**
     * Flag that indicates whether values should be written with tag or not;
     * false for packed arrays, true for others.
     */
    protected boolean _writeTag;
    
    /**
     * Flag that is set when the whole content is complete, can
     * be output.
     */
    protected boolean _complete;

    /**
     * Type of protobuf message that is currently being output: usually
     * matches write context, but for arrays may indicate "parent" of array.
     */
    protected ProtobufMessage _currentMessage = ROOT_MESSAGE;
    
    /**
     * Field to be output next; set when {@link JsonToken#FIELD_NAME} is written,
     * cleared once value has been written
     */
    protected ProtobufField _currField;
    
    /*
    /**********************************************************
    /* Output buffering
    /**********************************************************
     */

    /**
     * Ultimate destination
     */
    final protected OutputStream _output;

    /**
     * Current context, in form we can use it.
     */
    protected ProtobufWriteContext _pbContext;

    protected byte[] _currentBuffer;

    protected int _currentEnd;
    
    protected int _currentStart;
    
    protected int _currentPtr;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public ProtobufGenerator(IOContext ctxt, int jsonFeatures, int pbFeatures,
            ObjectCodec codec, OutputStream output)
        throws IOException
    {
        super(jsonFeatures, codec, BOGUS_WRITE_CONTEXT);
        _ioContext = ctxt;
        _protobufFeatures = pbFeatures;
        _output = output;
        _pbContext = ProtobufWriteContext.createNullContext();
        _currentBuffer = ctxt.allocWriteEncodingBuffer();
    }

    public void setSchema(ProtobufSchema schema)
    {
        if (_schema == schema) {
            return;
        }
        _schema = schema;
        // start with temporary root...
//        _currentContext = _rootContext = ProtobufWriteContext.createRootContext(this, schema);
        _pbContext = _rootContext = ProtobufWriteContext.createRootContext(schema.getRootType());
    }
    
    /*                                                                                       
    /**********************************************************                              
    /* Versioned                                                                             
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Overridden methods, configuration
    /**********************************************************
     */

    /**
     * Not sure whether to throw an exception or just do no-op; for now,
     * latter.
     */
    @Override
    public ProtobufGenerator useDefaultPrettyPrinter() {
        return this;
    }

    @Override
    public ProtobufGenerator setPrettyPrinter(PrettyPrinter pp) {
        return this;
    }

    @Override
    public Object getOutputTarget() {
        return _output;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof ProtobufSchema);
    }
    
    @Override public ProtobufSchema getSchema() {
        return _schema;
    }
    
    @Override
    public void setSchema(FormatSchema schema)
    {
        if (!(schema instanceof ProtobufSchema)) {
            throw new IllegalArgumentException("Can not use FormatSchema of type "
                    +schema.getClass().getName());
        }
        setSchema((ProtobufSchema) schema);
    }
    
    /*
    /**********************************************************************
    /* Overridden methods; writing field names
    /**********************************************************************
     */
    
    /* And then methods overridden to make final, streamline some
     * aspects...
     */

    @Override
    public final void writeFieldName(String name) throws IOException {
        _findField(name);
    }

    @Override
    public final void writeFieldName(SerializableString name) throws IOException {
        _findField(name.getValue());
    }

    @Override
    public final void writeStringField(String fieldName, String value) throws IOException {
        _findField(fieldName);
        writeString(value);
    }

    private final void _findField(String id) throws IOException
    {
        if (!_inObject) {
            _reportError("Can not write field name: current context not an OBJECT but "+_pbContext.getTypeDesc());
        }
        if (_currField != null) {
            _reportError("Can not write field name: expecting value (for field '"+_currField.name+"'");
        }
        ProtobufField f = _currentMessage.field(id);
        if (f == null) {
            // May be ok, if we have said so
            if ((_currentMessage == UNKNOWN_MESSAGE)
                    || Feature.IGNORE_UNKNOWN_FIELDS.enabledIn(_features)) {
                f = UNKNOWN_FIELD;
            } else {
                _reportError("Unrecognized field '"+id+"' (in Message of type "+_currentMessage.getName()
                        +"); known fields are: "+_currentMessage.fieldsAsString());
                        
            }
        }
        _pbContext.setField(f);
        _currField = f;
    }
    
    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public ProtobufGenerator enable(Feature f) {
        _protobufFeatures |= f.getMask();
        return this;
    }

    public ProtobufGenerator disable(Feature f) {
        _protobufFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_protobufFeatures & f.getMask()) != 0;
    }

    public ProtobufGenerator configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Public API: low-level I/O
    /**********************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        _output.flush();
    }
    
    @Override
    public void close() throws IOException
    {
        super.close();
        if (isEnabled(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)) {
            ProtobufWriteContext ctxt;
            while ((ctxt = _pbContext) != null) {
                if (ctxt.inArray()) {
                    writeEndArray();
                } else if (ctxt.inObject()) {
                    writeEndObject();
                } else {
                    break;
                }
            }
        }
        // May need to finalize...
        if (!_complete) {
            _complete();
        }
        if (_output != null) {
            if (_ioContext.isResourceManaged() || isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
                _output.close();
            } else  if (isEnabled(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)) {
                // If we can't close it, we should at least flush
                _output.flush();
            }
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }

    /*
    /**********************************************************
    /* Public API: structural output
    /**********************************************************
     */
    
    @Override
    public final void writeStartArray() throws IOException
    {
        // First: arrays only legal as Message (~= Object) fields:
        if (!_inObject) {
            _reportError("Current context not an OBJECT, can not write arrays");
        }
        if (_currField == null) { // just a sanity check
            _reportError("Can not write START_ARRAY without field (message type "+_currentMessage.getName()+")");
        }
        if (!_currField.isArray()) {
            _reportError("Can not write START_ARRAY: field '"+_currField.name+"' not declared as 'repeated'");
        }
        
        // !!! TODO: packed vs unpacked?

        // NOTE: do NOT clear _currField
        
        _pbContext = _pbContext.createChildArrayContext();
        _writeTag = !_currField.packed;
    }
    
    @Override
    public final void writeEndArray() throws IOException
    {
        if (!_pbContext.inArray()) {
            _reportError("Current context not an ARRAY but "+_pbContext.getTypeDesc());
        }
        _pbContext = _pbContext.getParent();
        if (_pbContext.inRoot()) {
            if (!_complete) {
                _complete();
            }
            _inObject = false;
        } else {
            _inObject = _pbContext.inObject();
        }
        // no arrays inside arrays, so parent can't be array and so:
        _writeTag = true; 
    }

    @Override
    public final void writeStartObject() throws IOException
    {
        if (_currField == null) {
            _reportError("Can not write START_OBJECT without field (message type "+_currentMessage.getName()+")");
        }
        // but also, field value must be Message if so
        if (!_currField.isObject()) {
            _reportError("Can not write START_OBJECT: type of field '"+_currField.name+"' not Message but: "+_currField.type);
        }
        
        if (_inObject) {
            _pbContext = _pbContext.createChildObjectContext(_currField.getMessageType());
            _currField = null;
        } else { // must be array, then
            _pbContext = _pbContext.createChildObjectContext(_currField.getMessageType());
            // but do NOT clear next field here
        }
        // even if within array, object fields use tags
        _writeTag = true; 
    }

    @Override
    public final void writeEndObject() throws IOException
    {
        if (!_inObject) {
            _reportError("Current context not an object but "+_pbContext.getTypeDesc());
        }
        if (_currField != null) {
            _reportError("Can not write END_OBJECT after writing FIELD_NAME ('"+_currField.name+"') but not value");
        }
        _pbContext = _pbContext.getParent();
        if (_pbContext.inRoot() && !_complete) {
            _complete();
        }
        _currField = _pbContext.getField();
        // possible that we might be within array, which might be packed:
        boolean inObj = _pbContext.inArray();
        _inObject = inObj;
        _writeTag = inObj || !_currField.packed;
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException
    {
        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite();
        // !!! TODO:
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException
    {
        // !!! TODO:
    }

    @Override
    public final void writeString(SerializableString sstr) throws IOException
    {
        byte[] b = sstr.asUnquotedUTF8();
        // !!! TODO:
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len) throws IOException
    {
        // !!! TODO:
    }

    @Override
    public final void writeUTF8String(byte[] text, int offset, int len) throws IOException
    {
        // !!! TODO:
    }

    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */
    
    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        // ok, better just Base64 encode as a String...
        if (offset > 0 || (offset+len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset+len);
        }
        final int end = offset+len;
        if (offset != 0 || end != data.length) {
            // !!! TODO:
        } else {
            // !!! TODO:
        }

        //        String encoded = b64variant.encode(data);
//        _writeScalar(encoded, "byte[]", STYLE_BASE64);
    }

    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException
    {
        _verifyValueWrite();
        FieldType t = _currField.type;
        if (t == FieldType.BOOLEAN) {
            
        }
    }

    @Override
    public void writeNull() throws IOException
    {
        _verifyValueWrite();

        // protobuf has no way of writing null does it?
        // ...but should we try to add placeholders in arrays?
    }

    @Override
    public void writeNumber(int i) throws IOException
    {
    }

    @Override
    public void writeNumber(long l) throws IOException
    {
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException
    {
        if (v == null) {
            writeNull();
            return;
        }
        // !!! TODO: better scheme to detect overflow or something
        writeNumber(v.longValue());
    }
    
    @Override
    public void writeNumber(double d) throws IOException
    {
    }    

    @Override
    public void writeNumber(float f) throws IOException
    {
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException
    {
        if (v == null) {
            writeNull();
            return;
        }
        // !!! TODO: better handling here... exception or write as string or... ?
        writeNumber(v.doubleValue());
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        throw new UnsupportedOperationException("Can not write 'untyped' numbers");
    }

    protected final void _verifyValueWrite() throws IOException {
        if (_currField == null) {
            _reportError("Can not write value without indicating field first (in message of type "+_currentMessage.getName()+")");
        }
    }
    
    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */
    
    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException {
        _throwInternal();
    }

    @Override
    protected void _releaseBuffers() {
        byte[] b = _currentBuffer;
        if (b != null) {
            _ioContext.releaseWriteEncodingBuffer(b);
            _currentBuffer = null;
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected void _complete() throws IOException
    {
        _complete = true;
        /*
        BinaryEncoder encoder = AvroSchema.encoder(_output);
        _rootContext.complete(encoder);
        encoder.flush();
        */
    }
}
