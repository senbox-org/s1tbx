package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;


abstract class AbstractSequence extends AbstractCollection implements SequenceInstance {
    private final SequenceType sequenceType;

    protected AbstractSequence(DataContext context, CollectionData parent, SequenceType sequenceType) {
        super(context, parent);
        this.sequenceType = sequenceType;
    }

    @Override
    public SequenceType getSequenceType() {
        return sequenceType;
    }

    @Override
    public CompoundInstance getCompound() {
        throw new DataAccessException();
    }

    @Override
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
        SequenceType mappedSequenceType = sequenceType;
        if (mappedSequenceType instanceof VarSequenceType) {
            VarSequenceType varSequenceType = (VarSequenceType) mappedSequenceType;
            mappedSequenceType = varSequenceType.resolve(parent);
            // WRITE PROBLEM
            if (mappedSequenceType != null) {
                mappedSequenceType = mapSequenceType(parent, mappedSequenceType);
            }
        }
        return mappedSequenceType;
    }
}