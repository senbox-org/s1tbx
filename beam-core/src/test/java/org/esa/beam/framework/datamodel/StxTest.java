package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.junit.Test;

import static org.junit.Assert.*;

public class StxTest {


    @Test
    public void testGetMedian() throws Exception {
        final RasterDataNode raster = createTestRaster(100, 120);
        final Stx stx = Stx.create(raster, 0, ProgressMonitor.NULL);
        assertEquals(5999.5, stx.getMedian(), 1.0e-6);
    }

    @Test
    public void testGetMedian_withGapsInHistogram() throws Exception {
        final RasterDataNode raster = createTestRaster(10, 12);
        final Stx stx = Stx.create(raster, 0, ProgressMonitor.NULL);
        assertEquals(59.5, stx.getMedian(), 1.0e-6);
    }

    @Test
    public void testGetMedian_NoDataValue() throws Exception {
        final RasterDataNode raster = createTestRaster(100, 120);
        raster.setNoDataValue(7000.0);
        raster.setNoDataValueUsed(true);
        final Stx stx = Stx.create(raster, raster.getValidMaskImage(), ProgressMonitor.NULL);
        assertEquals(5999.5, stx.getMedian(), 1.0e-6);
    }

    private RasterDataNode createTestRaster(int sceneWidth, int sceneHeight) {
        final Product product = new Product("dummy", "t", sceneWidth, sceneHeight);
        final VirtualBand raster = new VirtualBand("test", ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight,
                                                   "Y * " + sceneWidth + " + X");
        product.addBand(raster);
        return raster;
    }

}
