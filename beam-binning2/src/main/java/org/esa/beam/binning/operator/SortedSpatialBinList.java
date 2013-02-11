package org.esa.beam.binning.operator;

import org.esa.beam.binning.SpatialBin;

import java.util.Iterator;
import java.util.List;

public interface SortedSpatialBinList {

    Iterator<List<SpatialBin>> values();

    long  size();

    boolean isEmpty();

    void clear();

}
