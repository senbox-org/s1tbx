package org.esa.s1tbx.sar.gpf.geometric;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestDEM2 {

    private static int productWidth;
    private static int productHeight;
    private static Product sourceProduct;
    private static ElevationModelDescriptor srtmDescriptor;
    private static ElevationModel srtmDem;

    @BeforeClass
    public static void setUp() {
        productWidth = 10;
        productHeight = 10;
        sourceProduct = TestUtils.createProduct("GRD", productWidth, productHeight);

        srtmDescriptor = ElevationModelRegistry.getInstance().getDescriptor("SRTM 3Sec");
        srtmDem = srtmDescriptor.createDem(Resampling.NEAREST_NEIGHBOUR);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testPositionsUsingDEM() throws Exception {


        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();

        GeoPos geoPosUL = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        assertEquals(1924.39318847, srtmDem.getElevation(geoPosUL), 1e-8);

        GeoPos geoPosUR = geoCoding.getGeoPos(new PixelPos(productWidth, 0), null);
        assertEquals(1937.62377929, srtmDem.getElevation(geoPosUR), 1e-8);

        GeoPos geoPosLL = geoCoding.getGeoPos(new PixelPos(0, productHeight), null);
        assertEquals(564.75238037, srtmDem.getElevation(geoPosLL), 1e-8);

        GeoPos geoPosLR = geoCoding.getGeoPos(new PixelPos(productWidth, productHeight), null);
        assertEquals(2460.29052734, srtmDem.getElevation(geoPosLR), 1e-8);

        GeoPos geoPosCenter = geoCoding.getGeoPos(new PixelPos(productWidth / 2.0, productHeight / 2.0), null);
        assertEquals(3010.99121093, srtmDem.getElevation(geoPosCenter), 1e-8);

    }

    @Test
    public void testPositionsUsingProductReader() throws IOException {

        File demInstallDir = srtmDescriptor.getDemInstallDir();
        File demFile = new File(demInstallDir, "srtm_39_03.zip");

        Product demProduct = ProductIO.readProduct(demFile);
        Band demBand = demProduct.getBandAt(0);

        GeoCoding sourceGC = sourceProduct.getSceneGeoCoding();
        GeoCoding demGC = demProduct.getSceneGeoCoding();

        PixelPos ppUL = getDemPixelPos(sourceGC, demGC, 0, 0);
        assertEquals(1875, demBand.getSampleFloat((int) ppUL.getX(), (int) ppUL.getY()), 1.0e-6);

        PixelPos ppUR = getDemPixelPos(sourceGC, demGC, productWidth, 0);
        assertEquals(1888, demBand.getSampleFloat((int) ppUR.getX(), (int) ppUR.getY()), 1.0e-6);

        PixelPos ppLL = getDemPixelPos(sourceGC, demGC, 0, productHeight);
        assertEquals(515, demBand.getSampleFloat((int) ppLL.getX(), (int) ppLL.getY()), 1.0e-6);

        PixelPos ppLR = getDemPixelPos(sourceGC, demGC, productWidth, productHeight);
        assertEquals(2410, demBand.getSampleFloat((int) ppLR.getX(), (int) ppLR.getY()), 1.0e-6);

        PixelPos ppCenter = getDemPixelPos(sourceGC, demGC, (int) (productWidth / 2.0), (int) (productHeight / 2.0));
        assertEquals(2961, demBand.getSampleFloat((int) ppCenter.getX(), (int) ppCenter.getY()), 1.0e-6);

    }

    private PixelPos getDemPixelPos(GeoCoding sourceGC, GeoCoding demGC, int x, int y) {
        GeoPos geoPosUR = sourceGC.getGeoPos(new PixelPos(x, y), null);
        PixelPos pixelPos = demGC.getPixelPos(geoPosUR, null);
        pixelPos.setLocation(Math.round(pixelPos.getX()), Math.round(pixelPos.getY()));
        return pixelPos;
    }
}
