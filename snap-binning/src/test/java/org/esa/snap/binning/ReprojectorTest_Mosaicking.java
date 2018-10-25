package org.esa.snap.binning;

import org.esa.snap.binning.support.CrsGrid;
import org.esa.snap.binning.support.CrsGridEpsg3067;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ReprojectorTest_Mosaicking {

    private final BinManager binManager = new BinManager();


    private MosaickingGrid mosaickingGrid;
    private int width;
    private ReprojectorTest.NobsRaster raster;
    private Reprojector reprojector;

    @Before
    public void setUp() throws Exception {
        mosaickingGrid = new CrsGridEpsg3067(60);
        assertEquals(6, mosaickingGrid.getNumBins());

        assertEquals(2, mosaickingGrid.getNumCols(0));//  0...1 --> 0
        assertEquals(2, mosaickingGrid.getNumCols(1));//  2...3 --> 1
        assertEquals(2, mosaickingGrid.getNumCols(2)); // 4...5 --> 2

        int width = mosaickingGrid.getNumCols(0);
        int height = mosaickingGrid.getNumRows();
        assertEquals(2, width);
        assertEquals(3, height);

        raster = new ReprojectorTest.NobsRaster(new Rectangle(width, height));
        assertEquals("" +
                     "--\n" +
                     "--\n" +
                     "--\n",
                     raster.toString());

        reprojector = new Reprojector(mosaickingGrid, raster);
        reprojector.begin();
    }

    @Test
    public void testProcessBins_Full() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 0; i < mosaickingGrid.getNumBins(); i++) {
            bins.add(createTBin(i));
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "**\n" +
                     "**\n" +
                     "**\n",
                     raster.toString());
    }

    /*
     * Creates a test bin whose #obs = ID.
     */
    TemporalBin createTBin(int idx) {
        TemporalBin temporalBin = binManager.createTemporalBin(idx);
        temporalBin.setNumObs(idx);
        return temporalBin;
    }


}
