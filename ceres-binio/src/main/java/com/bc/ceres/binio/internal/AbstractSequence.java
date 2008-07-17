package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;


abstract class AbstractSequence extends AbstractCollection implements SequenceInstance {
    private final SequenceType sequenceType;

    protected AbstractSequence(IOContext context, CollectionData parent, SequenceType sequenceType) {
        super(context, parent);
        this.sequenceType = sequenceType;
    }

    public SequenceType getSequenceType() {
        return sequenceType;
    }

    public CompoundInstance getCompound() {
        throw new DataAccessException();
    }

    public SequenceInstance getSequence() {
        return this;
    }

    public SequenceType resolveSequenceType() throws IOException {
        SequenceType unresolvedSequenceType = getSequenceType();
        if (unresolvedSequenceType.isSizeKnown()) {
            return unresolvedSequenceType;
        }
        final SequenceType resolvedSequenceType = mapSequenceType(getParent(), unresolvedSequenceType);
        if (resolvedSequenceType == null || !resolvedSequenceType.isSizeKnown()) {
            throw new DataAccessException("failed to resolve type " + unresolvedSequenceType);
        }
        return resolvedSequenceType;
    }

    static SequenceType mapSequenceType(CollectionData parent, SequenceType sequenceType) throws IOException {
        final SequenceType mappedSequenceType;
        SequenceTypeMapper mapper = parent.getContext().getFormat().getSequenceTypeMapper(sequenceType);
        if (mapper != null) {
            mappedSequenceType = mapper.mapSequenceType(parent, sequenceType);
        } else {
            mappedSequenceType = sequenceType;
        }
        return mappedSequenceType;
    }
}