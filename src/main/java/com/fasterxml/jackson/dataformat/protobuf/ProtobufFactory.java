package com.fasterxml.jackson.dataformat.protobuf;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;

public class ProtobufFactory extends JsonFactory
{
    private static final long serialVersionUID = 1;

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */
    
    public ProtobufFactory() { }

    public ProtobufFactory(ObjectCodec codec) {
        super(codec);
    }
    protected ProtobufFactory(ProtobufFactory src, ObjectCodec oc)
    {
        super(src, oc);
        // TODO: copy feature flags, once those are added
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
    public String getFormatName()
    {
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
    
}
