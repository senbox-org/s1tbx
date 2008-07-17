package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;


final class VarSequenceOfFixCollections extends AbstractSequenceOfFixCollections {
    private SequenceType resolvedSequenceType;

    public VarSequenceOfFixCollections(IOContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType, position);
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    @Override
    protected SequenceType getResolvedSequenceType() {
        return resolvedSequenceType;
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    public boolean isSizeResolved(int index) {
        return resolvedSequenceType != null && resolvedSequenceType.isSizeKnown();
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    public void resolveSize(int index) throws IOException {
        resolveSize();
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    public void resolveSize() throws IOException {
        if (resolvedSequenceType == null) {
            resolvedSequenceType = resolveSequenceType();
            if (!resolvedSequenceType.isSizeKnown()) {
                throw new DataAccessException(toString());
            }
        }
    }

    public void flush() throws IOException {
        // todo - flush modified elements
    }
}