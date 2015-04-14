package org.esa.snap.binning;

import java.util.ArrayList;

class MyBinManager extends BinManager {
    ArrayList<SpatialBin> producedSpatialBins = new ArrayList<SpatialBin>();

    public MyBinManager(VariableContext variableContext, Aggregator... aggregators) {
        super(variableContext, aggregators);
    }

    @Override
    public SpatialBin createSpatialBin(long binIndex) {
        SpatialBin spatialBin = super.createSpatialBin(binIndex);
        producedSpatialBins.add(spatialBin);
        return spatialBin;
    }

}
