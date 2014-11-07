package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Type;
import com.squareup.protoparser.MessageType.Field;

/**
 * Stateful class needed to properly resolve type definitions of
 * protobuf message (and related types); some complexity coming
 * from possible nested nature of definitions.
 */
public class TypeResolver
{
    private final TypeResolver _parent;

    private Map<String,MessageType> _nativeMessageTypes;

    private Map<String,ProtobufEnum> _enumTypes;

    private Map<String,ProtobufMessage> _resolvedMessageTypes;
    
    protected TypeResolver(TypeResolver p, Map<String,MessageType> nativeMsgs,
            Map<String,ProtobufEnum> enums)
    {
        _parent = p;
        if (enums == null) {
            enums = Collections.emptyMap();
        }
        _enumTypes = enums;
        if (nativeMsgs == null) {
            nativeMsgs = Collections.emptyMap();
        }
        _nativeMessageTypes = nativeMsgs;
        _resolvedMessageTypes = Collections.emptyMap();
    }

    public static TypeResolver construct(List<Type> nativeTypes) {
        return construct(null, nativeTypes);
    }
    
    protected static TypeResolver construct(TypeResolver parent, List<Type> nativeTypes)
    {
        Map<String,MessageType> nativeMessages = null;
        Map<String,ProtobufEnum> enumTypes = null;
        
        for (Type nt : nativeTypes) {
            if (nt instanceof MessageType) {
                if (nativeMessages == null) {
                    nativeMessages = new LinkedHashMap<String,MessageType>();
                }
                nativeMessages.put(nt.getName(), (MessageType) nt);
            } else if (nt instanceof EnumType) {
                if (enumTypes == null) {
                    enumTypes = new LinkedHashMap<String,ProtobufEnum>();
                }
                enumTypes.put(nt.getName(), _constructEnum((EnumType) nt));
            } // no other known types?
        }
        return new TypeResolver(parent, nativeMessages, enumTypes);
    }

    protected static ProtobufEnum _constructEnum(EnumType nativeEnum)
    {
        final Map<String,Integer> valuesByName = new LinkedHashMap<String,Integer>();
        for (EnumType.Value v : nativeEnum.getValues()) {
            valuesByName.put(v.getName(), v.getTag());
        }
        return new ProtobufEnum(nativeEnum.getName(), valuesByName);
    }

    public ProtobufMessage resolve(MessageType rawType)
    {
        ProtobufMessage msg = _findResolvedMessage(rawType.getName());
        if (msg != null) {
            return msg;
        }
        /* Since MessageTypes can contain other type definitions, it is
         * important that we actually create a new context, that is,
         * new TypeResolver instance, and call resolution on that.
         */
        return TypeResolver.construct(this, rawType.getNestedTypes())
                ._resolve(rawType);
    }
        
    public ProtobufMessage _resolve(MessageType rawType)
    {
        List<ProtobufField> fields = new ArrayList<ProtobufField>();
        List<Unresolved> unresolved = null;

        for (Field f : rawType.getFields()) {
            String typeStr = f.getType();
            // First: could it be we have a simple scalar type
            FieldType type = FieldTypes.findType(typeStr);
            if (type != null) { // simple type
                fields.add(new ProtobufField(f, type));
                continue;
            }
            // If not, a resolved local definition?
            ProtobufField resolvedF = _findLocalResolved(f, typeStr);
            if (resolvedF != null) {
                fields.add(resolvedF);
                continue;
            }
            // or, barring that local but as of yet unresolved message?
            MessageType nativeMt = _nativeMessageTypes.get(typeStr);
            if (nativeMt == null) {
                // If not, perhaps parent might have an answer?
                if (_parent != null) {
                    ProtobufField f2 = _parent._findAnyResolved(f, typeStr);
                    if (f2 != null) {
                        fields.add(f2);
                        continue;
                    }
                }
                // Ok, we are out of options here...
                StringBuilder enumStr = _knownEnums(new StringBuilder());
                StringBuilder msgStr = _knownMsgs(new StringBuilder());
                throw new IllegalArgumentException("Unknown protobuf field type '"+typeStr
                        +"' for field '"+f.getName()+"' of MessageType '"+rawType.getName()
                        +"' (known enum types: "+enumStr+"; known message types: "+msgStr+")");
            }
            ProtobufField pf = new ProtobufField(f, FieldType.MESSAGE);
            fields.add(pf);
            if (unresolved == null) {
                unresolved = new ArrayList<Unresolved>();
            }
            unresolved.add(new Unresolved(pf, nativeMt));
        }
        ProtobufMessage resolved = new ProtobufMessage(rawType.getName(), fields);
        if (_resolvedMessageTypes.isEmpty()) {
            _resolvedMessageTypes = new HashMap<String,ProtobufMessage>();
        }
        _resolvedMessageTypes.put(rawType.getName(), resolved);

        // and then resolve dependencies for remaining fields
        if (unresolved != null) {
            for (Unresolved u : unresolved) {
                ProtobufField f = u.field;
                if (f.type != FieldType.MESSAGE || (f.getMessageType() != null)) {
                    continue;
                }
                f.assignMessageType(resolve(u.nativeType));
            }
        }
        return resolved;
    }    

    protected ProtobufMessage _findResolvedMessage(String typeStr)
    {
        ProtobufMessage msg = _resolvedMessageTypes.get(typeStr);
        if ((msg == null) && (_parent !=null)) {
            return _parent._findResolvedMessage(typeStr);
        }
        return msg;
    }

    protected ProtobufField _findAnyResolved(Field nativeField, String typeStr)
    {
        ProtobufField f = _findLocalResolved(nativeField, typeStr);
        if ((f == null) && (_parent != null)) {
            return _parent._findAnyResolved(nativeField, typeStr);
        }
        return f;
    }

    protected StringBuilder _knownEnums(StringBuilder sb) {
        if (_parent != null) {
            sb = _parent._knownEnums(sb);
        }
        for (String name : _enumTypes.keySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }
        return sb;
    }

    protected StringBuilder _knownMsgs(StringBuilder sb) {
        if (_parent != null) {
            sb = _parent._knownMsgs(sb);
        }
        for (String name : _nativeMessageTypes.keySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }
        return sb;
    }
    
    protected ProtobufField _findLocalResolved(Field nativeField, String typeStr)
    {
        ProtobufMessage msg = _resolvedMessageTypes.get(typeStr);
        if (msg != null) {
            return new ProtobufField(nativeField, msg);
        }
        ProtobufEnum et = _enumTypes.get(typeStr);
        if (et != null) {
            return new ProtobufField(nativeField, et);
        }
        return null;
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
