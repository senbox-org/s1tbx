package com.bc.ceres.binio;

/**
 * A sequence of elements of same type.
 */
public interface SequenceData extends CollectionData {

    /**
     * @return The resolved instance type of the sequence.
     */
    SequenceType getSequenceType();

    // todo - get<type>s(index, array, off, len)
    // todo - set<type>s(index, array, off, len)
}
