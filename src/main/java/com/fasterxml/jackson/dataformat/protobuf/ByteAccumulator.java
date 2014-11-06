package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;

/**
 * Helper object used for buffering content for cases where we need (byte-)length prefixes
 * for content like packed arrays, embedded messages, Strings and (perhaps) binary content.
 */
public class ByteAccumulator
{
    protected final ByteAccumulator _parent;

    // !!! TEMP: inefficient, only used during initial prototyping
    protected final ByteArrayOutputStream _bytes = new ByteArrayOutputStream(200);
    
    public ByteAccumulator(ByteAccumulator p) {
        _parent = p;
    }

    public void append(byte[] buf, int offset, int len) {
        _bytes.write(buf, offset, len);
    }

    public ByteAccumulator finish(OutputStream out) throws IOException
    {
        byte[] payload = _bytes.toByteArray();
        int plen = payload.length;
        byte[] lbytes = ProtobufUtil.lengthAsBytes(plen);
        
        // root? Just output it all 
        if (_parent == null) {
            out.write(lbytes);
            out.write(lbytes);
        } else {
            _parent.append(lbytes, 0, lbytes.length);
            _parent.append(payload, 0, payload.length);
        }
        return _parent;
    }
}
