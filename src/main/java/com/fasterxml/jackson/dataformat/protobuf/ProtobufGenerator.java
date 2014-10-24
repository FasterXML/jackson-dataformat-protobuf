package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
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
        
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    };
    
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

    protected ProtobufSchema _rootSchema;

    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * Reference to the root context since that is needed for serialization
     */
    protected ProtobufWriteContext _rootContext;

    /**
     * Flag that is set when the whole content is complete, can
     * be output.
     */
    protected boolean _complete;

    /**
     * Low-level id of the field
     */
    protected int _fieldId;
    
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
     * Current context
     */
    protected ProtobufWriteContext _currentContext;

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
        super(jsonFeatures, codec);
        _ioContext = ctxt;
        _protobufFeatures = pbFeatures;
        _output = output;
        _currentContext = ProtobufWriteContext.createNullContext();
        _currentBuffer = ctxt.allocWriteEncodingBuffer();
    }

    public void setSchema(ProtobufSchema schema)
    {
        if (_rootSchema == schema) {
            return;
        }
        _rootSchema = schema;
        // start with temporary root...
//        _currentContext = _rootContext = ProtobufWriteContext.createRootContext(this, schema);
        _currentContext = _rootContext = ProtobufWriteContext.createRootContext(this, schema);
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
        return _rootSchema;
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
        _currentContext.writeFieldName(name);
    }

    @Override
    public final void writeFieldName(SerializableString name) throws IOException {
        _currentContext.writeFieldName(name.getValue());
    }

    @Override
    public final void writeStringField(String fieldName, String value) throws IOException {
        _currentContext.writeFieldName(fieldName);
        writeString(value);
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
            while ((ctxt = _currentContext) != null) {
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
        _currentContext = _currentContext.createChildArrayContext();
    }
    
    @Override
    public final void writeEndArray() throws IOException
    {
        if (!_currentContext.inArray()) {
            _reportError("Current context not an ARRAY but "+_currentContext.getTypeDesc());
        }
        _currentContext = _currentContext.getParent();
        if (_currentContext.inRoot() && !_complete) {
            _complete();
        }
    }

    @Override
    public final void writeStartObject() throws IOException
    {
        _currentContext = _currentContext.createChildObjectContext();
    }

    @Override
    public final void writeEndObject() throws IOException
    {
        if (!_currentContext.inObject()) {
            _reportError("Current context not an object but "+_currentContext.getTypeDesc());
        }
        if (!_currentContext.canClose()) {
            _reportError("Can not write END_OBJECT after writing FIELD_NAME but not value");
        }
        _currentContext = _currentContext.getParent();
        if (_currentContext.inRoot() && !_complete) {
            _complete();
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        if (text == null) {
            writeNull();
            return;
        }
        _currentContext.writeValue(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException
    {
        writeString(new String(text, offset, len));
    }

    @Override
    public final void writeString(SerializableString sstr)
        throws IOException
    {
        writeString(sstr.toString());
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)
        throws IOException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public final void writeUTF8String(byte[] text, int offset, int len)
        throws IOException
    {
        writeString(new String(text, offset, len, "UTF-8"));
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
            _currentContext.writeValue(Arrays.copyOfRange(data, offset, end));
        } else {
            _currentContext.writeValue(data);
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
        _currentContext.writeValue(state ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public void writeNull() throws IOException
    {
        _currentContext.writeValue(null);
    }

    @Override
    public void writeNumber(int i) throws IOException
    {
        _currentContext.writeValue(Integer.valueOf(i));
    }

    @Override
    public void writeNumber(long l) throws IOException
    {
        _currentContext.writeValue(Long.valueOf(l));
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _currentContext.writeValue(v);
    }
    
    @Override
    public void writeNumber(double d) throws IOException
    {
        _currentContext.writeValue(Double.valueOf(d));
    }    

    @Override
    public void writeNumber(float f) throws IOException
    {
        _currentContext.writeValue(Float.valueOf(f));
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _currentContext.writeValue(dec);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Can not write 'untyped' numbers");
    }

    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */
    
    @Override
    protected final void _verifyValueWrite(String typeMsg)
        throws IOException
    {
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
