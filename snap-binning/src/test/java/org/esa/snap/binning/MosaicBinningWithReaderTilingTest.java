package org.esa.snap.binning;

import org.esa.snap.binning.support.PlateCarreeGrid;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class MosaicBinningWithReaderTilingTest {

    // This test reproduces the problem described in SNAP-1068

    private File inputFile;

    @Before
    public void setUp() {
        Locale.setDefault(Locale.ENGLISH);
        String testFile = "G:\\EOData\\_UserSupport\\MBoettcher\\ReproBinnningCalvalusIssue\\L2_of_S3A_OL_1_EFR____20170630T185820_20170630T190020_20171020T121102_0119_019_227______MR1_R_NT_002.SEN3L2_of_.nc";
        inputFile = new File(testFile);
        // File is on server of BC at EOData/related/snap/SNAP-1068
        // Should be reproducible with any other NetCDF files which use PixelGeoCoding and if the geo-region is adapted. Maybe not even NetCDF is important
        Assume.assumeTrue("Adapt path to input file in order to run this test", inputFile.exists());
    }

    @Test
    public void testMosaickingSteps() throws IOException {
        // commenting the following two lines makes the test succeed
        // working: 10, 100, 128, 250, 256, 400, 500, 513
        // not working: 512, 1024
        System.getProperties().setProperty("snap.dataio.reader.tileHeight", "512");
        System.getProperties().setProperty("snap.dataio.reader.tileWidth", "512");

        Product inputProduct = ProductIO.readProduct(inputFile);

        Map<String, Object> subsetParams = new HashMap<>();
        subsetParams.put("region", "1823,0,58,76");
        Product subset = GPF.createProduct("Subset", subsetParams, inputProduct);

        VirtualBand band = new VirtualBand("_binning_mask", ProductData.TYPE_UINT8,
                                           subset.getSceneRasterWidth(), subset.getSceneRasterHeight(),
                                           "true");
        subset.addBand(band);


        MosaickingGrid planetaryGrid;

        // PlateCareeGrid is dependent on the tileSize
        planetaryGrid = new PlateCarreeGrid(64800);
        // CrsGrid is independent, it works in both cases
//        planetaryGrid = new CrsGrid(64800, "EPSG:4326");
        Product reproProduct = planetaryGrid.reprojectToGrid(subset);

        // better not - it is big
//        ProductIO.writeProduct(reproProduct, "G:\\temp\\test\\test_subset_repro.dim", "BEAM-DIMAP");

        Map<String, Object> subsetParams2 = new HashMap<>();
        String geometryWkt = "POLYGON((-124 39.9,-124 40.2,-123.9 40.2,-123.9 39.9,-124 39.9))";
        subsetParams2.put("geoRegion", geometryWkt);
        Product subset2 = GPF.createProduct("Subset", subsetParams2, reproProduct);

        assertEquals(1, subset2.getBand("_binning_mask").getSampleInt(10, 40));
        assertEquals(Float.NaN, subset2.getBand("sdr_1").getSampleFloat(4, 27), 1.0e-3);
        assertEquals(0.01509, subset2.getBand("sdr_1").getSampleFloat(5, 27), 1.0e-3);
        assertEquals(0.04478, subset2.getBand("sdr_1").getSampleFloat(0, 78), 1.0e-3);
        assertEquals(0.01380, subset2.getBand("sdr_1").getSampleFloat(14, 93), 1.0e-3);
        assertEquals(Float.NaN, subset2.getBand("sdr_1").getSampleFloat(14, 94), 1.0e-3);
        assertEquals(0.015046, subset2.getBand("sdr_1").getSampleFloat(20, 60), 1.0e-3);

        // enable following line to look at results
        ProductIO.writeProduct(subset2, "G:\\temp\\test\\test_subset_repro_subset_from_nc.dim", "BEAM-DIMAP");
    }
}