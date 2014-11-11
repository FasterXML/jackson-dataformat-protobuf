package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;
import java.net.URL;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;

public class ProtobufFactory extends JsonFactory
{
    private static final long serialVersionUID = 1;

    /*
    /**********************************************************
    /* Constants
    /**********************************************************
     */

    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
//    final static int DEFAULT_PBUF_PARSER_FEATURE_FLAGS = ProtobufParser.Feature.collectDefaults();
    final static int DEFAULT_PBUF_PARSER_FEATURE_FLAGS = 0;

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_PBUF_GENERATOR_FEATURE_FLAGS = ProtobufGenerator.Feature.collectDefaults();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected int _formatParserFeatures;
    protected int _formatGeneratorFeatures;
    
    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */
    
    public ProtobufFactory() { }

    public ProtobufFactory(ObjectCodec codec) {
        super(codec);
        _formatParserFeatures = DEFAULT_PBUF_PARSER_FEATURE_FLAGS;
        _formatGeneratorFeatures = DEFAULT_PBUF_GENERATOR_FEATURE_FLAGS;
    }
    protected ProtobufFactory(ProtobufFactory src, ObjectCodec oc)
    {
        super(src, oc);
        _formatParserFeatures = src._formatParserFeatures;
        _formatGeneratorFeatures = src._formatGeneratorFeatures;
    }

    @Override
    public ProtobufFactory copy()
    {
        _checkInvalidCopy(ProtobufFactory.class);
        return new ProtobufFactory(this, null);
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    @Override
    protected Object readResolve() {
        return new ProtobufFactory(this, _objectCodec);
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
    /* Format detection functionality
    /**********************************************************
     */
    
    @Override
    public String getFormatName() {
        return ProtobufSchema.FORMAT_NAME_PROTOBUF;
    }
    
    /**
     * Sub-classes need to override this method
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // TODO, if possible... probably isn't?
        return MatchStrength.INCONCLUSIVE;
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    // Protobuf is not positional
    @Override
    public boolean requiresPropertyOrdering() {
        return false;
    }

    // Protobuf can embed raw binary data natively
    @Override
    public boolean canHandleBinaryNatively() {
        return true;
    }

    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link ProtobufParser.Feature} for list of features)
     */
    public final ProtobufFactory configure(ProtobufParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link ProtobufParser.Feature} for list of features)
     */
    public ProtobufFactory enable(ProtobufParser.Feature f) {
        _formatParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link ProtobufParser.Feature} for list of features)
     */
    public ProtobufFactory disable(ProtobufParser.Feature f) {
        _formatParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(ProtobufParser.Feature f) {
        return (_formatParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link ProtobufGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public final ProtobufFactory configure(ProtobufGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link ProtobufGenerator.Feature} for list of features)
     */
    public ProtobufFactory enable(ProtobufGenerator.Feature f) {
        _formatGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link ProtobufGenerator.Feature} for list of features)
     */
    public ProtobufFactory disable(ProtobufGenerator.Feature f) {
        _formatGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(ProtobufGenerator.Feature f) {
        return f.enabledIn(_formatGeneratorFeatures);
    }

    /*
    /**********************************************************
    /* Overridden parser factory method
    /**********************************************************
     */

    @Override
    public ProtobufParser createParser(File f) throws IOException {
        return _createParser(new FileInputStream(f), _createContext(f, true));
    }

    @Override
    public ProtobufParser createParser(URL url) throws IOException {
        return _createParser(_optimizedStreamFromURL(url), _createContext(url, true));
    }

    @Override
    public ProtobufParser createParser(InputStream in) throws IOException {
        return _createParser(in, _createContext(in, false));
    }

    @Override
    public ProtobufParser createParser(byte[] data) throws IOException {
        return _createParser(data, 0, data.length, _createContext(data, true));
    }

    @Override
    public ProtobufParser createParser(byte[] data, int offset, int len) throws IOException {
        return _createParser(data, offset, len, _createContext(data, true));
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods
    /**********************************************************
     */

    @Override
    public ProtobufGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        out = _decorate(out, ctxt);
        return _createProtobufGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec, out);
    }

    /**
     * Method for constructing {@link JsonGenerator} for generating
     * CBOR-encoded output.
     *<p>
     * Since CBOR format always uses UTF-8 internally, no encoding need
     * to be passed to this method.
     */
    @Override
    public ProtobufGenerator createGenerator(OutputStream out) throws IOException {
        IOContext ctxt = _createContext(out, false);
        out = _decorate(out, ctxt);
        return _createProtobufGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec, out);
    }

    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return super._createContext(srcRef, resourceManaged);
    }

    @Override
    protected ProtobufParser _createParser(InputStream in, IOContext ctxt) throws IOException
    {
        byte[] buf = ctxt.allocReadIOBuffer();
        return new ProtobufParser(ctxt, _parserFeatures, _formatParserFeatures,
                _objectCodec, in, buf, 0, 0, true);
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return _nonByteSource();
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException {
        return _nonByteSource();
    }

    @Override
    protected ProtobufParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException
    {
        return new ProtobufParser(ctxt, _parserFeatures, _formatParserFeatures,
                _objectCodec, null, data, offset, len, false);
    }

    @Override
    protected ProtobufGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return _nonByteTarget();
    }

    @Override
    protected ProtobufGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return _createProtobufGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec, out);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException {
        return _nonByteTarget();
    }

    private final ProtobufGenerator _createProtobufGenerator(IOContext ctxt,
            int stdFeat, int formatFeat, ObjectCodec codec, OutputStream out) throws IOException
    {
        return new ProtobufGenerator(ctxt, stdFeat, formatFeat, _objectCodec, out);
    }
    
    protected <T> T _nonByteTarget() {
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }

    protected <T> T _nonByteSource() {
        throw new UnsupportedOperationException("Can not create generator for non-byte-based source");
    }
    
}
