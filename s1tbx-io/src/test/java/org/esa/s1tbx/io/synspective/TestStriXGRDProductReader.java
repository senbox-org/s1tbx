
package org.esa.s1tbx.io.synspective;

import org.esa.s1tbx.commons.test.MetadataValidator;
import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 */
public class TestStriXGRDProductReader extends ReaderTest {

    final static File inputSMGGRDMeta = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SM_GRD_GeoTIFF_202111_044_2021-11-11T133724Z/PAR-20211111133724_SMGRD.xml");
    final static File inputSMGRDFolder = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SM_GRD_GeoTIFF_202111_044_2021-11-11T133724Z");

    final static File inputSLGGRDMeta = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SL_GRD_GeoTIFF_202202-00075_2022-02-15T143911Z/PAR-20220215143910_SLGRD.xml");
    final static File inputSLGRDFolder = new File(S1TBXTests.inputPathProperty + "/SAR/Synspective/GRD/STRIX-A_SL_GRD_GeoTIFF_202202-00075_2022-02-15T143911Z");

    final static MetadataValidator.Options options = new MetadataValidator.Options();

    public TestStriXGRDProductReader() {
        super(new StriXGRDProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputSMGGRDMeta + " not found", inputSMGGRDMeta.exists());
        assumeTrue(inputSMGRDFolder + " not found", inputSMGRDFolder.exists());
        assumeTrue(inputSLGGRDMeta + " not found", inputSLGGRDMeta.exists());
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
        Product prod = testReader(inputSMGGRDMeta.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }

    @Test
    public void testOpeningSL_GRD_Folder() throws Exception {
        Product prod = testReader(inputSLGRDFolder.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }

    @Test
    public void testOpeningSL_GRD_Metadata() throws Exception {
        Product prod = testReader(inputSLGGRDMeta.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata(options);
        validator.validateBands(new String[] {"Amplitude_VV", "Intensity_VV"});
    }
}
