package com.fasterxml.jackson.dataformat.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;

public class ProtobufParser extends ParserMinimalBase
{
    // State constants
    
    // State right after parser created; may start root Object
    private final static int STATE_INITIAL = 0;

    // State in which we expect another root-object entry key
    private final static int STATE_ROOT_KEY = 1;

    // State after STATE_ROOT_KEY, when we are about to get a value
    // (scalar or structured)
    private final static int STATE_ROOT_VALUE = 2;

    // State after either reaching end-of-input, or getting explicitly closed
    private final static int STATE_CLOSED = 3;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    protected ProtobufSchema _schema;
    
    /*
    /**********************************************************
    /* Generic I/O state
    /**********************************************************
     */

    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader.
     */
    final protected IOContext _ioContext;

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /*
    /**********************************************************
    /* Current input data
    /**********************************************************
     */

    // Note: type of actual buffer depends on sub-class, can't include

    /**
     * Pointer to next available character in buffer
     */
    protected int _inputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int _inputEnd = 0;

    /*
    /**********************************************************
    /* Current input location information
    /**********************************************************
     */

    /**
     * Number of characters/bytes that were contained in previous blocks
     * (blocks that were already processed prior to the current buffer).
     */
    protected long _currInputProcessed = 0L;

    /**
     * Current row location of current point in input buffer, starting
     * from 1, if available.
     */
    protected int _currInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int _currInputRowStart = 0;

    /*
    /**********************************************************
    /* Information about starting location of event
    /* Reader is pointing to; updated on-demand
    /**********************************************************
     */

    // // // Location info at point when current token was started

    /**
     * Total number of bytes/characters read before start of current token.
     * For big (gigabyte-sized) sizes are possible, needs to be long,
     * unlike pointers and sizes related to in-memory buffers.
     */
    protected long _tokenInputTotal = 0; 

    /**
     * Input row on which current token starts, 1-based
     */
    protected int _tokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int _tokenInputCol = 0;
    
    /*
    /**********************************************************
    /* Parsing state
    /**********************************************************
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _parsingContext;

    /**
     * Buffer that contains contents of String values, including
     * field names if necessary (name split across boundary,
     * contains escape sequence, or access needed to char array)
     */
    protected final TextBuffer _textBuffer;

    /**
     * Temporary buffer that is needed if field name is accessed
     * using {@link #getTextCharacters} method (instead of String
     * returning alternatives)
     */
    protected char[] _nameCopyBuffer = null;

    /**
     * Flag set to indicate whether the field name is available
     * from the name copy buffer or not (in addition to its String
     * representation  being available via read context)
     */
    protected boolean _nameCopied = false;
    
    /**
     * ByteArrayBuilder is needed if 'getBinaryValue' is called. If so,
     * we better reuse it for remainder of content.
     */
    protected ByteArrayBuilder _byteArrayBuilder = null;

    /**
     * We will hold on to decoded binary data, for duration of
     * current event, so that multiple calls to
     * {@link #getBinaryValue} will not need to decode data more
     * than once.
     */
    protected byte[] _binaryValue;

    /*
    /**********************************************************
    /* Input source config, state (from ex StreamBasedParserBase)
    /**********************************************************
     */

    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     */
    protected InputStream _inputStream;

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected byte[] _inputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     *<p>
     * If it is not, it also means that parser can NOT modify underlying
     * buffer.
     */
    protected boolean _bufferRecyclable;

    /*
    /**********************************************************
    /* Additional parsing state
    /**********************************************************
     */

    /**
     * Flag that indicates that the current token has not yet
     * been fully processed, and needs to be finished for
     * some access (or skipped to obtain the next token)
     */
    protected boolean _tokenIncomplete = false;

    /**
     * Current state of the parser.
     */
    protected int _state = STATE_INITIAL;

    /**
     * The innermost Object type ("message" in proto lingo) we are handling.
     */
    protected ProtobufMessage _currentType;
    
    /*
    /**********************************************************
    /* Numeric conversions
    /**********************************************************
     */

    final protected static int NR_UNKNOWN = 0;

    // First, integer types

    final protected static int NR_INT = 0x0001;
    final protected static int NR_LONG = 0x0002;
    final protected static int NR_BIGINT = 0x0004;

    // And then floating point types

    final protected static int NR_DOUBLE = 0x008;
    final protected static int NR_BIGDECIMAL = 0x0010;

    // Also, we need some numeric constants

    final static BigInteger BI_MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    final static BigInteger BI_MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);

    final static BigInteger BI_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    final static BigInteger BI_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    
    final static BigDecimal BD_MIN_LONG = new BigDecimal(BI_MIN_LONG);
    final static BigDecimal BD_MAX_LONG = new BigDecimal(BI_MAX_LONG);

    final static BigDecimal BD_MIN_INT = new BigDecimal(BI_MIN_INT);
    final static BigDecimal BD_MAX_INT = new BigDecimal(BI_MAX_INT);

    final static long MIN_INT_L = (long) Integer.MIN_VALUE;
    final static long MAX_INT_L = (long) Integer.MAX_VALUE;

    // These are not very accurate, but have to do... (for bounds checks)

    final static double MIN_LONG_D = (double) Long.MIN_VALUE;
    final static double MAX_LONG_D = (double) Long.MAX_VALUE;

    final static double MIN_INT_D = (double) Integer.MIN_VALUE;
    final static double MAX_INT_D = (double) Integer.MAX_VALUE;
    
    /**
     * Bitfield that indicates which numeric representations
     * have been calculated for the current type
     */
    protected int _numTypesValid = 0;

    // First primitives

    protected int _numberInt;
    protected long _numberLong;
    protected double _numberDouble;

    // And then object types

    protected BigInteger _numberBigInt;
    protected BigDecimal _numberBigDecimal;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public ProtobufParser(IOContext ctxt, int parserFeatures,
            ObjectCodec codec,
            InputStream in, byte[] inputBuffer, int start, int end,
            boolean bufferRecyclable)
    {
        super(parserFeatures);
        _ioContext = ctxt;
        _objectCodec = codec;

        _inputStream = in;
        _inputBuffer = inputBuffer;
        _inputPtr = start;
        _inputEnd = end;
        _bufferRecyclable = bufferRecyclable;
        _textBuffer = ctxt.constructTextBuffer();
        _parsingContext = JsonReadContext.createRootContext(null);

        _tokenInputRow = -1;
        _tokenInputCol = -1;
    }

    public void setSchema(ProtobufSchema schema)
    {
        if (_schema == schema) {
            return;
        }
        if (_state != STATE_INITIAL) {
            throw new IllegalStateException("Can not change Schema after parsing has started");
        }
        _schema = schema;
        // start with temporary root...
//        _currentContext = _rootContext = ProtobufWriteContext.createRootContext(this, schema);
    }

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
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
    /* Abstract impls
    /**********************************************************
     */

    @Override
    public int releaseBuffered(OutputStream out) throws IOException
    {
        int count = _inputEnd - _inputPtr;
        if (count < 1) {
            return 0;
        }
        // let's just advance ptr to end
        int origPtr = _inputPtr;
        out.write(_inputBuffer, origPtr, count);
        return count;
    }
    
    @Override
    public Object getInputSource() {
        return _inputStream;
    }

    /**
     * Overridden since we do not really have character-based locations,
     * but we do have byte offset to specify.
     */
    @Override
    public JsonLocation getTokenLocation()
    {
        // token location is correctly managed...
        return new JsonLocation(_ioContext.getSourceReference(),
                _tokenInputTotal, // bytes
                -1, -1, (int) _tokenInputTotal); // char offset, line, column
    }   

    /**
     * Overridden since we do not really have character-based locations,
     * but we do have byte offset to specify.
     */
    @Override
    public JsonLocation getCurrentLocation()
    {
        final long offset = _currInputProcessed + _inputPtr;
        return new JsonLocation(_ioContext.getSourceReference(),
                offset, // bytes
                -1, -1, (int) offset); // char offset, line, column
    }

    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    @Override
    public String getCurrentName() throws IOException
    {
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JsonReadContext parent = _parsingContext.getParent();
            return parent.getCurrentName();
        }
        return _parsingContext.getCurrentName();
    }

    @Override
    public void overrideCurrentName(String name)
    {
        // Simple, but need to look for START_OBJECT/ARRAY's "off-by-one" thing:
        JsonReadContext ctxt = _parsingContext;
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            ctxt = ctxt.getParent();
        }
        // Unfortunate, but since we did not expose exceptions, need to wrap
        try {
            ctxt.setCurrentName(name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void close() throws IOException {
        _state = STATE_CLOSED;
        _currToken = null;
        if (!_closed) {
            _closed = true;
            try {
                _closeInput();
            } finally {
                // as per [JACKSON-324], do in finally block
                // Also, internal buffer(s) can now be released as well
                _releaseBuffers();
            }
        }
    }

    @Override
    public boolean isClosed() { return _closed; }

    @Override
    public JsonReadContext getParsingContext() {
        return _parsingContext;
    }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

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
    
    @Override
    public boolean hasTextCharacters()
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textBuffer.hasTextAsCharacters();
        }
        if (_currToken == JsonToken.FIELD_NAME) {
            return _nameCopied;
        }
        return false;
    }

    protected void _releaseBuffers() throws IOException
    {
         if (_bufferRecyclable) {
             byte[] buf = _inputBuffer;
             if (buf != null) {
                 _inputBuffer = null;
                 _ioContext.releaseReadIOBuffer(buf);
             }
         }
         _textBuffer.releaseBuffers();
         char[] buf = _nameCopyBuffer;
         if (buf != null) {
             _nameCopyBuffer = null;
             _ioContext.releaseNameCopyBuffer(buf);
         }
    }

    /*
    /**********************************************************
    /* JsonParser impl
    /**********************************************************
     */

    @Override
    public JsonToken nextToken() throws IOException
    {
        switch (_state) {
        case STATE_INITIAL:
            _currentType = _schema.getRootType();
            _state = STATE_ROOT_KEY;
            _parsingContext = _parsingContext.createChildObjectContext(-1, -1);            
            return (_currToken = JsonToken.START_OBJECT);

        case STATE_ROOT_KEY:
            // end-of-the-line?
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    close();
                    return null;
                }
            }
            int tag = _decodeTag();
            
        case STATE_ROOT_VALUE:

        case STATE_CLOSED:
            return null;
        }
        // !!! TBI
        return null;
    }

    /*
    /**********************************************************
    /* Public API, traversal, nextXxxValue/nextFieldName
    /**********************************************************
     */

    // !!! TODO: implement efficiently?
    /*
    @Override
    public boolean nextFieldName(SerializableString str) throws IOException
    {
    }
    */

    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
     */

    /**
     * Method for accessing textual representation of the current event;
     * if no current event (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any event.
     */
    @Override    
    public String getText() throws IOException
    {
        if (_tokenIncomplete) {
            _finishToken();
        }
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textBuffer.contentsAsString();
        }
        JsonToken t = _currToken;
        if (t == null) { // null only before/after document
            return null;
        }
        if (t == JsonToken.FIELD_NAME) {
            return _parsingContext.getCurrentName();
        }
        if (t.isNumeric()) {
            return getNumberValue().toString();
        }
        return _currToken.asString();
    }

    @Override
    public char[] getTextCharacters() throws IOException
    {
        if (_currToken != null) { // null only before/after document
            if (_tokenIncomplete) {
                _finishToken();
            }
            switch (_currToken) {                
            case VALUE_STRING:
                return _textBuffer.getTextBuffer();
            case FIELD_NAME:
                return _parsingContext.getCurrentName().toCharArray();
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return getNumberValue().toString().toCharArray();
                
            default:
                return _currToken.asCharArray();
            }
        }
        return null;
    }

    @Override    
    public int getTextLength() throws IOException
    {
        if (_currToken != null) { // null only before/after document
            if (_tokenIncomplete) {
                _finishToken();
            }
            switch (_currToken) {
            case VALUE_STRING:
                return _textBuffer.size();                
            case FIELD_NAME:
                return _parsingContext.getCurrentName().length();
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return getNumberValue().toString().length();
                
            default:
                return _currToken.asCharArray().length;
            }
        }
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override
    public String getValueAsString() throws IOException
    {
        if (_currToken != JsonToken.VALUE_STRING) {
            if (_currToken == null || _currToken == JsonToken.VALUE_NULL || !_currToken.isScalarValue()) {
                return null;
            }
        }
        return getText();
    }

    @Override
    public String getValueAsString(String defaultValue) throws IOException
    {
        if (_currToken != JsonToken.VALUE_STRING) {
            if (_currToken == null || _currToken == JsonToken.VALUE_NULL || !_currToken.isScalarValue()) {
                return defaultValue;
            }
        }
        return getText();
    }

    /*
    /**********************************************************
    /* Public API, access to token information, binary
    /**********************************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException
    {
        if (_tokenIncomplete) {
            _finishToken();
        }
        if (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT ) {
            // TODO, maybe: support base64 for text?
            _reportError("Current token ("+_currToken+") not VALUE_EMBEDDED_OBJECT, can not access as binary");
        }
        return _binaryValue;
    }

    @Override
    public Object getEmbeddedObject() throws IOException
    {
        if (_tokenIncomplete) {
            _finishToken();
        }
        if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT ) {
            return _binaryValue;
        }
        return null;
    }

    @Override
    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException
    {
        if (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT ) {
            _reportError("Current token ("+_currToken+") not VALUE_EMBEDDED_OBJECT, can not access as binary");
        }

        // !!! TBI
        return -1;
    }

    /*
    /**********************************************************
    /* Numeric accessors of public API
    /**********************************************************
     */
    
    @Override
    public Number getNumberValue() throws IOException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _checkNumericValue(NR_UNKNOWN); // will also check event type
        }
        // Separate types for int types
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return _numberInt;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return _numberLong;
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                return _numberBigInt;
            }
            // Shouldn't get this far but if we do
            return _numberBigDecimal;
        }
    
        /* And then floating point types. But here optimal type
         * needs to be big decimal, to avoid losing any data?
         */
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _numberBigDecimal;
        }
        if ((_numTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return _numberDouble;
    }
    
    @Override
    public NumberType getNumberType() throws IOException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _checkNumericValue(NR_UNKNOWN); // will also check event type
        }
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return NumberType.INT;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return NumberType.LONG;
            }
            return NumberType.BIG_INTEGER;
        }
    
        /* And then floating point types. Here optimal type
         * needs to be big decimal, to avoid losing any data?
         * However... using BD is slow, so let's allow returning
         * double as type if no explicit call has been made to access
         * data as BD?
         */
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return NumberType.BIG_DECIMAL;
        }
        return NumberType.DOUBLE;
    }
    
    @Override
    public int getIntValue() throws IOException
    {
        if ((_numTypesValid & NR_INT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) { // not parsed at all
                _checkNumericValue(NR_INT); // will also check event type
            }
            if ((_numTypesValid & NR_INT) == 0) { // wasn't an int natively?
                convertNumberToInt(); // let's make it so, if possible
            }
        }
        return _numberInt;
    }
    
    @Override
    public long getLongValue() throws IOException
    {
        if ((_numTypesValid & NR_LONG) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _checkNumericValue(NR_LONG);
            }
            if ((_numTypesValid & NR_LONG) == 0) {
                convertNumberToLong();
            }
        }
        return _numberLong;
    }
    
    @Override
    public BigInteger getBigIntegerValue() throws IOException
    {
        if ((_numTypesValid & NR_BIGINT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _checkNumericValue(NR_BIGINT);
            }
            if ((_numTypesValid & NR_BIGINT) == 0) {
                convertNumberToBigInteger();
            }
        }
        return _numberBigInt;
    }
    
    @Override
    public float getFloatValue() throws IOException
    {
        double value = getDoubleValue();
        /* 22-Jan-2009, tatu: Bounds/range checks would be tricky
         *   here, so let's not bother even trying...
         */
        /*
        if (value < -Float.MAX_VALUE || value > MAX_FLOAT_D) {
            _reportError("Numeric value ("+getText()+") out of range of Java float");
        }
        */
        return (float) value;
    }
    
    @Override
    public double getDoubleValue() throws IOException
    {
        if ((_numTypesValid & NR_DOUBLE) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _checkNumericValue(NR_DOUBLE);
            }
            if ((_numTypesValid & NR_DOUBLE) == 0) {
                convertNumberToDouble();
            }
        }
        return _numberDouble;
    }
    
    @Override
    public BigDecimal getDecimalValue() throws IOException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _checkNumericValue(NR_BIGDECIMAL);
            }
            if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
                convertNumberToBigDecimal();
            }
        }
        return _numberBigDecimal;
    }

    /*
    /**********************************************************
    /* Numeric conversions
    /**********************************************************
     */    

    protected void _checkNumericValue(int expType) throws IOException
    {
        // Int or float?
        if (_currToken == JsonToken.VALUE_NUMBER_INT || _currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            return;
        }
        _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
    }

    protected void convertNumberToInt() throws IOException
    {
        // First, converting from long ought to be easy
        if ((_numTypesValid & NR_LONG) != 0) {
            // Let's verify it's lossless conversion by simple roundtrip
            int result = (int) _numberLong;
            if (((long) result) != _numberLong) {
                _reportError("Numeric value ("+getText()+") out of range of int");
            }
            _numberInt = result;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            if (BI_MIN_INT.compareTo(_numberBigInt) > 0 
                    || BI_MAX_INT.compareTo(_numberBigInt) < 0) {
                reportOverflowInt();
            }
            _numberInt = _numberBigInt.intValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_INT_D || _numberDouble > MAX_INT_D) {
                reportOverflowInt();
            }
            _numberInt = (int) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_INT.compareTo(_numberBigDecimal) > 0 
                || BD_MAX_INT.compareTo(_numberBigDecimal) < 0) {
                reportOverflowInt();
            }
            _numberInt = _numberBigDecimal.intValue();
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_INT;
    }
    
    protected void convertNumberToLong() throws IOException
    {
        if ((_numTypesValid & NR_INT) != 0) {
            _numberLong = (long) _numberInt;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            if (BI_MIN_LONG.compareTo(_numberBigInt) > 0 
                    || BI_MAX_LONG.compareTo(_numberBigInt) < 0) {
                reportOverflowLong();
            }
            _numberLong = _numberBigInt.longValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_LONG_D || _numberDouble > MAX_LONG_D) {
                reportOverflowLong();
            }
            _numberLong = (long) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_LONG.compareTo(_numberBigDecimal) > 0 
                || BD_MAX_LONG.compareTo(_numberBigDecimal) < 0) {
                reportOverflowLong();
            }
            _numberLong = _numberBigDecimal.longValue();
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_LONG;
    }
    
    protected void convertNumberToBigInteger() throws IOException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            // here it'll just get truncated, no exceptions thrown
            _numberBigInt = _numberBigDecimal.toBigInteger();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberInt);
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            _numberBigInt = BigDecimal.valueOf(_numberDouble).toBigInteger();
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_BIGINT;
    }
    
    protected void convertNumberToDouble() throws IOException
    {
        // Note: this MUST start with more accurate representations, since we don't know which
        //  value is the original one (others get generated when requested)
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            _numberDouble = _numberBigDecimal.doubleValue();
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberDouble = _numberBigInt.doubleValue();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberDouble = (double) _numberLong;
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberDouble = (double) _numberInt;
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_DOUBLE;
    }
    
    protected void convertNumberToBigDecimal() throws IOException
    {
        // Note: this MUST start with more accurate representations, since we don't know which
        //  value is the original one (others get generated when requested)
        if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Let's parse from String representation, to avoid rounding errors that
            //non-decimal floating operations would incur
            _numberBigDecimal = NumberInput.parseBigDecimal(getText());
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberBigDecimal = new BigDecimal(_numberBigInt);
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberInt);
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_BIGDECIMAL;
    }

    protected void reportOverflowInt() throws IOException {
        _reportError("Numeric value ("+getText()+") out of range of int ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
    }
    
    protected void reportOverflowLong() throws IOException {
        _reportError("Numeric value ("+getText()+") out of range of long ("+Long.MIN_VALUE+" - "+Long.MAX_VALUE+")");
    }    
    
    /*
    /**********************************************************
    /* Internal methods, secondary parsing
    /**********************************************************
     */

    /**
     * Method called to finish parsing of a token so that token contents
     * are retriable
     */
    protected void _finishToken() throws IOException
    {
        // !!! TBI
    }

    /*
    /**********************************************************
    /* Low-level reading, other
    /**********************************************************
     */

    protected final boolean loadMore() throws IOException
    {
        _currInputProcessed += _inputEnd;
        
        if (_inputStream != null) {
            int count = _inputStream.read(_inputBuffer, 0, _inputBuffer.length);
            if (count > 0) {
                _inputPtr = 0;
                _inputEnd = count;
                return true;
            }
            // End of input
            _closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("InputStream.read() returned 0 characters when trying to read "+_inputBuffer.length+" bytes");
            }
        }
        return false;
    }

    protected final void loadMoreGuaranteed() throws IOException {
        if (!loadMore()) { _reportInvalidEOF(); }
    }
    
    /**
     * Helper method that will try to load at least specified number bytes in
     * input buffer, possible moving existing data around if necessary
     */
    protected final void _loadToHaveAtLeast(int minAvailable) throws IOException
    {
        // No input stream, no leading (either we are closed, or have non-stream input source)
        if (_inputStream == null) {
            throw _constructError("Needed to read "+minAvailable+" bytes, reached end-of-input");
        }
        // Need to move remaining data in front?
        int amount = _inputEnd - _inputPtr;
        if (amount > 0 && _inputPtr > 0) {
            _currInputProcessed += _inputPtr;
            //_currInputRowStart -= _inputPtr;
            System.arraycopy(_inputBuffer, _inputPtr, _inputBuffer, 0, amount);
            _inputEnd = amount;
        } else {
            _inputEnd = 0;
        }
        _inputPtr = 0;
        while (_inputEnd < minAvailable) {
            int count = _inputStream.read(_inputBuffer, _inputEnd, _inputBuffer.length - _inputEnd);
            if (count < 1) {
                // End of input
                _closeInput();
                // Should never return 0, so let's fail
                if (count == 0) {
                    throw new IOException("InputStream.read() returned 0 characters when trying to read "+amount+" bytes");
                }
                throw _constructError("Needed to read "+minAvailable+" bytes, missed "+minAvailable+" before end-of-input");
            }
            _inputEnd += count;
        }
    }

    protected ByteArrayBuilder _getByteArrayBuilder() {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }

    protected void _closeInput() throws IOException {
        if (_inputStream != null) {
            if (_ioContext.isResourceManaged() || isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)) {
                _inputStream.close();
            }
            _inputStream = null;
        }
    }

    @Override
    protected void _handleEOF() throws JsonParseException {
        if (!_parsingContext.inRoot()) {
            _reportInvalidEOF(": expected close marker for "+_parsingContext.getTypeDesc()+" (from "+_parsingContext.getStartLocation(_ioContext.getSourceReference())+")");
        }
    }

    /*
    /**********************************************************
    /* Decoding
    /**********************************************************
     */

    protected int _decodeTag() throws IOException
    {
        // offline slow case
        if ((_inputPtr + 4)  >= _inputEnd) {
            return _decodeTagSlow();
        }
        int v = _inputBuffer[_inputPtr++];
        if (v < 0) { // keep going
            v = (v & 0x7F) << 7;
            
            // Tag VInts guaranteed to stay in 31 bits, i.e. no more than 5 bytes
            int ch = _inputBuffer[_inputPtr++];
            if (ch < 0) {
                v = (v | (ch & 0x7F)) << 7;
                ch = _inputBuffer[_inputPtr++];
                if (ch < 0) {
                    v = (v | (ch & 0x7F)) << 7;
                    ch = _inputBuffer[_inputPtr++];
                    if (ch < 0) {
                        v = (v | (ch & 0x7F)) << 7;

                        // and now the last byte; at most 3 bits
                        int last = _inputBuffer[_inputPtr++] & 0xFF;
                        
                        if (last > 0x7) {
                            _reportTooLongVInt(last);
                        }
                        v |= last;
                    } else {
                        v |= ch;
                    }
                } else {
                    v |= ch;
                }
            } else {
                v |= ch;
            }
        }
        return v;
    }

    protected int _decodeTagSlow() throws IOException
    {
        int v = 0;
        int i = 0;

        while (true) {
            v <<= 7;
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            int ch = _inputBuffer[_inputPtr++];
            if (++i == 5) { // must end
                ch &= 0xFF;
                if (ch > 0x7) {
                    _reportTooLongVInt(ch);
                }
            }
            if (ch >= 0) {
                v |= ch;
                return (v | ch);
            }
            v |= (ch & 0x7F);
        }
    }

    protected void _reportTooLongVInt(int fifth) throws IOException
    {
        _reportError("Too long tag VInt: fifth byte 0x"+Integer.toHexString(fifth));
    }
}
