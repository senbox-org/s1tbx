package com.bc.ceres.binio.util;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SequenceTypeMapper;
import com.bc.ceres.binio.Type;

import java.io.IOException;

/**
 * An implementation of a {@link SequenceTypeMapper} which returns a {@link SequenceType}
 * with a number of elements computed by the abstract {@link #getElementCount} method.
 */
public abstract class SequenceElementCountResolver implements SequenceTypeMapper {

    public SequenceType mapSequenceType(CollectionData parentData, SequenceType sequenceType) throws IOException {
        final Type elementType = sequenceType.getElementType();
        final int elementCount = getElementCount(parentData, sequenceType);
        return new SequenceType(elementType, elementCount);
    }

    public abstract int getElementCount(CollectionData parent, SequenceType sequenceType) throws IOException;
}