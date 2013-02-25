package org.esa.beam.binning.operator;

import org.esa.beam.binning.SpatialBin;

import java.util.List;

interface SpatialBinCollection {

    Iterable<List<SpatialBin>> getCollectedBins();

    long size();

    boolean isEmpty();

    void clear();

}
