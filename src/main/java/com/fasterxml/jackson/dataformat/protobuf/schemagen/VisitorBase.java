package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWithSerializerProvider;

public abstract class VisitorBase
    implements JsonFormatVisitorWithSerializerProvider
{
    protected SerializerProvider _provider;

    @Override
    public final SerializerProvider getProvider() {
        return _provider;
    }

    @Override
    public final void setProvider(SerializerProvider provider) {
        _provider = provider;
    }
}
