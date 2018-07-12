package org.esa.snap.raster.gpf.texture;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.Test;

import java.awt.image.Raster;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import static org.junit.Assert.*;

public class GLCMOp_IntegrationTest {

    private final static String SOURCE_PRODUCT_NAME =
            "subset_0_of_S2B_MSIL1C_20170718T101029_N0205_R022_T34VCL_20170718T101346_idepix_c2rcc_normal.dim";

    @Test
    public void testGLCMOp_Integration() throws IOException {
        URL resource = getClass().getResource(SOURCE_PRODUCT_NAME);
        Product sourceProduct = ProductIO.readProduct(resource.getFile());

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "conc_chl,conc_chl_masked");

        Product glcmProduct = GPF.createProduct("GLCM", parameters, sourceProduct);

        assertNotNull(glcmProduct);
        assert(glcmProduct.getBandGroup().contains("conc_chl_Contrast"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_Dissimilarity"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_Homogeneity"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_ASM"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_Energy"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_MAX"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_Entropy"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_GLCMMean"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_GLCMVariance"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_GLCMCorrelation"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_Contrast"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_Dissimilarity"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_Homogeneity"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_ASM"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_Energy"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_MAX"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_Entropy"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_GLCMMean"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_GLCMVariance"));
        assert(glcmProduct.getBandGroup().contains("conc_chl_masked_GLCMCorrelation"));

        Band concChlContrastBand = glcmProduct.getBand("conc_chl_Contrast");
        Raster concChlContrastData = concChlContrastBand.getSourceImage().getData();
        assertEquals(0.0, concChlContrastData.getSampleDouble(5, 5, 0), 1e-8);
        assertEquals(15.342857360839844, concChlContrastData.getSampleDouble(200, 300, 0), 1e-8);
    }
}