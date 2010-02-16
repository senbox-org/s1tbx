package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;
import java.text.MessageFormat;


abstract class AbstractSequence extends AbstractCollection implements SequenceInstance {
    private final SequenceType sequenceType;

    protected AbstractSequence(DataContext context, CollectionData parent, SequenceType sequenceType) {
        super(context, parent);
        this.sequenceType = sequenceType;
    }

    @Override
    public SequenceType getType() {
        return sequenceType;
    }

    @Override
    @Deprecated
    public final SequenceType getSequenceType() {
        return getType();
    }

    @Override
    public CompoundInstance getCompound() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public SequenceInstance getSequence() {
        return this;
    }

    SequenceType resolveSequenceType() throws IOException {
        SequenceType unresolvedSequenceType = getType();
        if (unresolvedSequenceType.isSizeKnown()) {
            return unresolvedSequenceType;
        }
        final SequenceType resolvedSequenceType = resolveSequenceType(getParent(), unresolvedSequenceType);
        if (resolvedSequenceType == null || !resolvedSequenceType.isSizeKnown()) {
            String msg = MessageFormat.format("Failed to resolve type ''{0}''", unresolvedSequenceType);
            throw new DataAccessException(msg);
        }
        return resolvedSequenceType;
    }

    static SequenceType resolveSequenceType(CollectionData parent, SequenceType sequenceType) throws IOException {
        SequenceType resolvedSequenceType;
        if (sequenceType instanceof VarSequenceType) {
            resolvedSequenceType = ((VarSequenceType) sequenceType).resolve(parent);
            if (resolvedSequenceType == null) {
                throw new IllegalStateException("Sequence type resolved to null: " + sequenceType);
            }
            if (resolvedSequenceType == sequenceType) {
                throw new IllegalStateException("Sequence type resolved to itself: " + sequenceType);
            }
            if (resolvedSequenceType instanceof VarSequenceType) {
                resolvedSequenceType = resolveSequenceType(parent, resolvedSequenceType);
                if (resolvedSequenceType instanceof VarSequenceType) {
                    throw new IllegalStateException("Sequence type resolved to an unresolvable type: " + resolvedSequenceType);
                }
            }
        }  else {
            resolvedSequenceType = sequenceType;
        }
        return resolvedSequenceType;
    }
}