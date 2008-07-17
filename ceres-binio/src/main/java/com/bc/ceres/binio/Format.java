package com.bc.ceres.binio;

import com.bc.ceres.binio.util.SequenceElementCountResolver;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * A binary format.
 */
public class Format {
    private final CompoundType type;
    private String name;
    private String version;
    private ByteOrder byteOrder;
    private Map<SequenceType, SequenceTypeMapper> sequenceTypeResolverMap;

    public Format(CompoundType type) {
        this(type, ByteOrder.BIG_ENDIAN);
    }

    public Format(CompoundType type, ByteOrder byteOrder) {
        this.type = type;
        this.name = type.getName();
        this.version = "1.0.0";
        this.byteOrder = byteOrder;
        this.sequenceTypeResolverMap = new HashMap<SequenceType, SequenceTypeMapper>(16);
    }

    public CompoundType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public ByteOrder getByteOrder(Type type) {
        return byteOrder;
    }

    public void setByteOrder(Type type, ByteOrder byteOrder) {
        throw new IllegalStateException("not implemented yet");
    }

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