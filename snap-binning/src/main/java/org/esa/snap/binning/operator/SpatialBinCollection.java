package org.esa.snap.binning.operator;

import org.esa.snap.binning.SpatialBin;

import java.util.List;

/**
 * Implementations provide an {@link Iterable} for the bin collection and some meta-data of the collection.
 */
interface SpatialBinCollection {

    /**
     * Provides an {@link Iterable iterable} for the underlying collection of bins.
     *
     * @return The {@link Iterable iterable} for the collection of bins.
     */
    Iterable<List<SpatialBin>> getBinCollection();

    /**
     * Returns the size of the collection.
     *
     * @return the size of the collection
     */
    long size();

    /**
     * Returns {@code true} if the collections contains no bins.
     *
     * @return {@code true} if the collections contains no bins
     */
    boolean isEmpty();

}
