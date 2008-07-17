package com.bc.ceres.binio;

import java.io.IOException;

/**
 * Maps one {@link SequenceType} to another. May be used to resolve unknown element type
 * or unknown number of elements of a sequence.
 * {@code SequenceTypeMapper}s can be registered in a {@link IOContext}.
 *
 * @see com.bc.ceres.binio.util.SequenceElementCountResolver
 */
public interface SequenceTypeMapper {
    SequenceType mapSequenceType(CollectionData parent, SequenceType sequenceType) throws IOException;
}