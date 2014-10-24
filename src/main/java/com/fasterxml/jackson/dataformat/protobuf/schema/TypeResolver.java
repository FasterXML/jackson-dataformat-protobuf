package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Type;
import com.squareup.protoparser.MessageType.Field;

/**
 * Stateful class needed to properly resolved type definitions that
 * may be nested.
 */
public class TypeResolver
{
    private final TypeResolver parent;

    private final Map<String,MessageType> nativeMessageTypes;

    private final Map<String,ProtobufEnum> enums;

    private final Map<String,ProtobufMessage> resolvedTypes;
    
    public TypeResolver(TypeResolver p)
    {
        parent = p;
        nativeMessageTypes = null;
        enums = null;
        resolvedTypes = null;
    }

    public static TypeResolver construct(List<Type> nativeTypes) {
        return construct(null, nativeTypes);
    }
    
    protected static TypeResolver construct(TypeResolver parent, List<Type> nativeTypes)
    {
        LinkedHashMap<String,MessageType> nativeMessages = new LinkedHashMap<String,MessageType>();
        Map<String,ProtobufEnum> enumTypes = Collections.emptyMap();
        
        for (Type nt : nativeTypes) {
            if (nt instanceof MessageType) {
                nativeMessages.put(nt.getName(), (MessageType) nt);
            } else if (nt instanceof EnumType) {
                if (enumTypes.isEmpty()) {
                    enumTypes = new LinkedHashMap<String,ProtobufEnum>();
                }
                enumTypes.put(nt.getName(), _constructEnum((EnumType) nt));
            } // no other known types?
        }
        return new TypeResolver(parent);
    }

    public ProtobufMessage resolve(MessageType rawType)
    {
        // !!! TODO
        return null;
    }
    
    protected static ProtobufEnum _constructEnum(EnumType nativeEnum)
    {
        final Map<String,Integer> valuesByName = new LinkedHashMap<String,Integer>();
        for (EnumType.Value v : nativeEnum.getValues()) {
            valuesByName.put(v.getName(), v.getTag());
        }
        return new ProtobufEnum(nativeEnum.getName(), valuesByName);
    }
    
    protected static ProtobufMessage _resolveMessage(MessageType msgType,
            Map<String,MessageType> nativeMessageTypes,            
            Map<String,ProtobufEnum> enums,
            Map<String,ProtobufMessage> resolvedTypes)
    {
        ProtobufMessage msg = resolvedTypes.get(msgType.getName());
        if (msg != null) {
            return msg;
        }
        
        List<ProtobufField> fields = new ArrayList<ProtobufField>();
        List<Unresolved> unresolved = null;

        for (Field f : msgType.getFields()) {
            String typeStr = f.getType();
            FieldType type = FieldTypes.findType(typeStr);
            if (type != null) { // simple type
                fields.add(new ProtobufField(f, type));
                continue;
            }
            // enum, maybe?
            ProtobufEnum en = enums.get(typeStr);
            if (en != null) {
                fields.add(new ProtobufField(f, en));
                continue;
            }
            // if not, hopefully message?
            msg = resolvedTypes.get(typeStr);
            if (msg != null) {
                fields.add(new ProtobufField(f, msg));
                continue;
            }
            // otherwise, let's see if we have any idea
            MessageType nativeMt = nativeMessageTypes.get(typeStr);
            if (nativeMt == null) {
                String enumStr = enums.keySet().toString();
                String msgStr = nativeMessageTypes.keySet().toString();
                throw new IllegalArgumentException("Unknown protobuf field type '"+f.getType()
                        +"' for field '"+f.getName()+"' of MessageType '"+msgType.getName()
                        +"' (enums =  "+enumStr+"; message = "+msgStr+")");
            }
            ProtobufField pf = new ProtobufField(f, FieldType.MESSAGE);
            fields.add(pf);
            if (unresolved == null) {
                unresolved = new ArrayList<Unresolved>();
            }
            unresolved.add(new Unresolved(pf, nativeMt));
        }
        ProtobufMessage resolved = new ProtobufMessage(msgType.getName(), fields);
        resolvedTypes.put(resolved.getName(), resolved);

        // and then resolve dependencies for remaining columns
        for (Unresolved u : unresolved) {
            ProtobufField f = u.field;
            if (f.type != FieldType.MESSAGE || (f.getMessageType() != null)) {
                continue;
            }
            f.assignMessageType(_resolveMessage(u.nativeType,
                    nativeMessageTypes, enums, resolvedTypes));
        }
        return resolved;
    }    
    private final static class Unresolved
    {
        public final ProtobufField field;
        public final MessageType nativeType;

        public Unresolved(ProtobufField f, MessageType nt) {
            field = f;
            nativeType = nt;
        }
    }
}
