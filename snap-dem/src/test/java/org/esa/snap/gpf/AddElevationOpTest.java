package org.esa.snap.gpf;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class AddElevationOpTest {

    private static final String PRODUCT_NAME =
            "subset_0_of_MER_FR__1PNEPA20060730_093924_000000982049_00480_23079_0746.dim";
    private static final String DEM_NAME = "45N015E_5M.ACE2";

    @Test
    public void testAddingExternalDEM() throws IOException {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new AddElevationOp.Spi());
        final Map<String, Object> parametersMap = new HashMap<>();
        final File externalDEMFile = new File(AddElevationOpTest.class.getResource(DEM_NAME).getFile());
        parametersMap.put("demName", "External DEM");
        parametersMap.put("externalDEMFile", externalDEMFile);
        parametersMap.put("externalDEMNoDataValue", -500.0);
        parametersMap.put("demResamplingMethod", ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        final String pathToProduct = AddElevationOpTest.class.getResource(PRODUCT_NAME).getFile();
        final Product sourceProduct = ProductIO.readProduct(pathToProduct);
        final Product elevationProduct = GPF.createProduct("AddElevation", parametersMap, sourceProduct);

        final Band elevationBand = elevationProduct.getBand("elevation");
        assertNotNull(elevationBand);
        assertEquals(-500.0, elevationBand.getNoDataValue(), 1e-8);
        final float sampleFloat = elevationBand.getSampleFloat(37, 29);
        assertEquals(38.6651268, sampleFloat, 1e-8);
    }

} 