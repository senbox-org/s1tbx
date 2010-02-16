package com.bc.ceres.binio;

/**
 * A sequence of elements of same type.
 */
public interface SequenceData extends CollectionData {

    /**
     * @return The resolved instance type of the sequence.
     */
    @Override
    SequenceType getType();

    /**
     * @return The resolved instance type of the sequence.
     *
     * @deprecated since ceres 0.10; use {@link #getType()} instead.
     */
    @Deprecated
    SequenceType getSequenceType();

    // todo - get<type>s(index, array, off, len)
    // todo - set<type>s(index, array, off, len)
}
