package com.fasterxml.jackson.dataformat.protobuf.schemagen;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;

public class MessageElementVisitor
    extends VisitorBase
    implements JsonObjectFormatVisitor
{
    @Override
    public void property(BeanProperty writer) {
        // TODO Auto-generated method stub
    }

    @Override
    public void property(String name, JsonFormatVisitable handler,
            JavaType propertyTypeHint)
    {
    }

    @Override
    public void optionalProperty(BeanProperty writer) {
    }

    @Override
    public void optionalProperty(String name, JsonFormatVisitable handler,
            JavaType propertyTypeHint)
    {
    }
}
