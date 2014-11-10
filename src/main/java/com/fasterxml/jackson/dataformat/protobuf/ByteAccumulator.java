package com.fasterxml.jackson.dataformat.protobuf;

import java.io.*;

/**
 * Helper object used for buffering content for cases where we need (byte-)length prefixes
 * for content like packed arrays, embedded messages, Strings and (perhaps) binary content.
 */
public class ByteAccumulator
{
    protected final ByteAccumulator _parent;

    protected final int _typedTag;
    
    // !!! TEMP: inefficient, only used during initial prototyping
    protected final ByteArrayOutputStream _bytes = new ByteArrayOutputStream(200);

    public ByteAccumulator(ByteAccumulator p, int typedTag) {
        _parent = p;
        _typedTag = typedTag;
    }

    public ByteAccumulator(ByteAccumulator p) {
        _parent = p;
        _typedTag = -1;
    }

    public void append(byte[] buf, int offset, int len) {
        _bytes.write(buf, offset, len);
    }

    public ByteAccumulator finish(OutputStream out,
            byte[] input, int offset, int len) throws IOException
    {
        byte[] payload = _bytes.toByteArray();
        int plen = payload.length + len;
        byte[] lbytes = ProtobufUtil.lengthAsBytes(plen);
        
        // root? Just output it all 
        if (_parent == null) {
            if (_typedTag != -1) {
                byte[] tagBytes = ProtobufUtil.lengthAsBytes(_typedTag);
                out.write(tagBytes);
            }
            out.write(lbytes);
            out.write(payload);
            if (len > 0) {
                out.write(input, offset, len);
            }
        } else {
            if (_typedTag != -1) {
                byte[] tagBytes = ProtobufUtil.lengthAsBytes(_typedTag);
                _parent.append(tagBytes, 0, tagBytes.length);
            }
            _parent.append(lbytes, 0, lbytes.length);
            _parent.append(payload, 0, payload.length);
            _parent.append(input, offset, len);
        }
        return _parent;
    }
    
    public ByteAccumulator finish(OutputStream out) throws IOException
    {
        byte[] payload = _bytes.toByteArray();
        int plen = payload.length;
        byte[] tagBytes = ProtobufUtil.lengthAsBytes(_typedTag);
        byte[] lbytes = ProtobufUtil.lengthAsBytes(plen);
        
        // root? Just output it all 
        if (_parent == null) {
            out.write(tagBytes);
            out.write(lbytes);
            out.write(payload);
        } else {
            _parent.append(tagBytes, 0, tagBytes.length);
            _parent.append(lbytes, 0, lbytes.length);
            _parent.append(payload, 0, payload.length);
        }
        return _parent;
    }
}
