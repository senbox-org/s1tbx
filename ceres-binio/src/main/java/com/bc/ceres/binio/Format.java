package com.bc.ceres.binio;

import com.bc.ceres.binio.util.SequenceElementCountResolver;
import com.bc.ceres.core.Assert;

import java.io.IOException;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A binary format.
 */
public class Format {
    private CompoundType type;
    private String name;
    private String version;
    private ByteOrder byteOrder;
    private Map<SequenceType, SequenceTypeMapper> sequenceTypeResolverMap;
    private Map<String, Type> typeDefMap;

    public Format(CompoundType type) {
        this(type, ByteOrder.BIG_ENDIAN);
    }

    public Format(CompoundType type, ByteOrder byteOrder) {
        setType(type);
        setName(type.getName());
        setVersion("1.0.0");
        setByteOrder(byteOrder);
        this.sequenceTypeResolverMap = new HashMap<SequenceType, SequenceTypeMapper>(16);
        this.typeDefMap = new HashMap<String, Type>(16);
    }

    public CompoundType getType() {
        return type;
    }

    public void setType(CompoundType type) {
        Assert.notNull(type, "type");
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Assert.notNull(name, "name");
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        Assert.notNull(version, "version");
        this.version = version;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        Assert.notNull(byteOrder, "byteOrder");
        this.byteOrder = byteOrder;
    }

    public boolean isTypeDef(String name) {
        Assert.notNull(name, "name");
        return typeDefMap.containsKey(name);
    }

    public Type getTypeDef(String name) {
        Assert.notNull(name, "name");
        Type type = typeDefMap.get(name);
        if (type == null) {
            throw new IllegalArgumentException(MessageFormat.format("Type definition ''{0}'' not found", name));
        }
        return type;
    }

    public void addTypeDef(String name, Type type) {
        Assert.notNull(name, "name");
        Assert.notNull(type, "type");
        Type oldType = typeDefMap.get(name);
        if (oldType != null && !oldType.equals(type)) {
            throw new IllegalArgumentException(MessageFormat.format("Type definition ''{0}'' already exists as ''{1}''", name, oldType));
        }
        typeDefMap.put(name, type);
    }

    public Type removeTypeDef(String name) {
        Assert.notNull(name, "name");
        return typeDefMap.remove(name);
    }

    /////////////////////////////////////////////////////////////////////////
    // todo - Remove following API

    public void addSequenceTypeMapper(CompoundType.Member member, SequenceTypeMapper sequenceTypeMapper) {
        if (!(member.getType() instanceof SequenceType)) {
            throw new IllegalArgumentException("member");
        }
        addSequenceTypeMapper((SequenceType) member.getType(), sequenceTypeMapper);
    }

    public void addSequenceTypeMapper(SequenceType sequenceType, SequenceTypeMapper sequenceTypeMapper) {
        if (sequenceType.isSizeKnown()) {
            throw new IllegalArgumentException("sequenceType");
        }
        sequenceTypeResolverMap.put(sequenceType, sequenceTypeMapper);
    }

    public void addSequenceElementCountResolver(String compoundMemberName, String referencedMemberName) {
        addSequenceElementCountResolver(getType(), compoundMemberName, referencedMemberName);
    }

    public void addSequenceElementCountResolver(CompoundType compoundType, String compoundMemberName, String referencedMemberName) {
        // todo - check indexes
        final int sequenceMemberIndex = compoundType.getMemberIndex(compoundMemberName);
        final int elementCountMemberIndex = compoundType.getMemberIndex(referencedMemberName);
        final CompoundType.Member sequenceMember = compoundType.getMember(sequenceMemberIndex);
        // todo - check type
        final SequenceType sequenceType = (SequenceType) sequenceMember.getType();
        final SequenceElementCountResolver sequenceElementCountResolver = new SequenceElementCountResolver() {
            @Override
            public int getElementCount(CollectionData parent, SequenceType unresolvedSequenceType) throws IOException {
                return parent.getInt(elementCountMemberIndex);
            }
        };
        addSequenceTypeMapper(sequenceType, sequenceElementCountResolver);
    }

    public SequenceTypeMapper getSequenceTypeMapper(SequenceType sequenceType) {
        return sequenceTypeResolverMap.get(sequenceType);
    }
}