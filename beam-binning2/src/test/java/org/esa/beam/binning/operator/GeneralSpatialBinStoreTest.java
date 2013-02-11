package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class GeneralSpatialBinStoreTest {

    @Test
    public void testConsumeSpatialBins() throws Exception {

        GeneralSpatialBinStore store = new GeneralSpatialBinStore();

        BinningContext ctx = Mockito.mock(BinningContext.class);
        ArrayList<SpatialBin> spatialBins = new ArrayList<SpatialBin>();
        spatialBins.add(createSpatialBin(23));
        store.consumeSpatialBins(ctx, spatialBins);
        store.consumingCompleted();
        SortedSpatialBinList binMap = store.getSpatialBinMap();
        Iterator<List<SpatialBin>> actualBins = binMap.values();
        List<SpatialBin> binList = actualBins.next();
        assertEquals(23, binList.get(0).getIndex());

    }

    private SpatialBin createSpatialBin(long binIndex) {
        return new SpatialBin(binIndex, 2);
    }
}
