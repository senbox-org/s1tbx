
package org.esa.s1tbx.io.strix.grd;

import org.esa.s1tbx.commons.test.MetadataValidator;
import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 */
public class TestStriXGRDProductReader extends ReaderTest {

    final static File inputSMGRDMeta = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SM_GRD_GeoTIFF_202111_044_2021-11-11T133724Z/PAR-20211111133724_SMGRD.xml");
    final static File inputSMGRDFolder = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SM_GRD_GeoTIFF_202111_044_2021-11-11T133724Z");

    final static File inputSLGRDMeta = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SL_GRD_GeoTIFF_202202-00075_2022-02-15T143911Z/PAR-20220215143910_SLGRD.xml");
    final static File inputSLGRDFolder = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SL_GRD_GeoTIFF_202202-00075_2022-02-15T143911Z");

    final static MetadataValidator.Options options = new MetadataValidator.Options();

    public TestStriXGRDProductReader() {
        super(new StriXGRDProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputSMGRDMeta + " not found", inputSMGRDMeta.exists());
        assumeTrue(inputSMGRDFolder + " not found", inputSMGRDFolder.exists());
        assumeTrue(inputSLGRDMeta + " not found", inputSLGRDMeta.exists());
        assumeTrue(inputSLGRDFolder + " not found", inputSLGRDFolder.exists());

        options.validateOrbitStateVectors = false;
        options.validateSRGR = false;
        options.validateDopplerCentroids = false;
    }

    @Test
    public void testOpeningSM_GRD_Folder() throws Exception {
        Product prod = testReader(inputSMGRDFolder.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }

    @Test
    public void testOpeningSM_GRD_Metadata() throws Exception {
        Product prod = testReader(inputSMGRDMeta.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }

    @Test
    @Ignore("range spacing missing in metadata")
    public void testOpeningSL_GRD_Folder() throws Exception {
        Product prod = testReader(inputSLGRDFolder.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }

    @Test
    @Ignore("range spacing missing in metadata")
    public void testOpeningSL_GRD_Metadata() throws Exception {
        Product prod = testReader(inputSLGRDMeta.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }
}
