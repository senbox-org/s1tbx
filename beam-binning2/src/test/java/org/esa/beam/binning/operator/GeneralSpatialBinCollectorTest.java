package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.binning.support.SEAGrid;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GeneralSpatialBinCollectorTest {

    @Test
    public void testConsumeSpatialBins() throws Exception {

        final SEAGrid seaGrid = new SEAGrid(10);
        GeneralSpatialBinCollector store = new GeneralSpatialBinCollector(seaGrid.getNumBins());
        try {
            BinningContext ctx = Mockito.mock(BinningContext.class);
            Mockito.when(ctx.getPlanetaryGrid()).thenReturn(seaGrid);

            ArrayList<SpatialBin> spatialBins = new ArrayList<SpatialBin>();
            spatialBins.add(createSpatialBin(23));
            store.consumeSpatialBins(ctx, spatialBins);
            store.consumingCompleted();
            SpatialBinCollection binMap = store.getSpatialBinCollection();
            Iterable<List<SpatialBin>> actualBinLists = binMap.getBinCollection();
            List<SpatialBin> binList = actualBinLists.iterator().next();
            assertEquals(23, binList.get(0).getIndex());
        } finally {
            store.close();
        }
    }

    private SpatialBin createSpatialBin(long binIndex) {
        return new SpatialBin(binIndex, 2);
    }
}
