
package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.commons.test.TestData;
import org.esa.s1tbx.io.AbstractProductReaderPlugInTest;
import org.esa.snap.core.dataio.ProductReader;
import org.junit.Test;

import static org.esa.s1tbx.io.sentinel1.TestSentinel1ProductReader.inputGRDFolder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSentinel1ProductReaderPlugIn extends AbstractProductReaderPlugInTest {

    public TestSentinel1ProductReaderPlugIn() {
        super(new Sentinel1ProductReaderPlugIn());
    }

    @Test
    public void testCreateReaderInstance() {
        ProductReader productReader = plugin.createReaderInstance();
        assertNotNull(productReader);
        assertTrue(productReader instanceof Sentinel1ProductReader);
    }

    @Test
    public void testGetFormatNames() {
        assertArrayEquals(new String[]{"SENTINEL-1"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() {
        assertArrayEquals(new String[]{".safe",".zip"}, plugin.getDefaultFileExtensions());
    }

    @Override
    protected String[] getValidPrimaryMetadataFileNames() {
        return new String[] {"manifest.safe"};
    }

    @Override
    protected String[] getInvalidPrimaryMetadataFileNames() {
        return new String[] {"manifest.xml"};
    }

    @Test
    public void testValidDecodeQualification() {
        isValidDecodeQualification(TestData.inputS1_GRD);
        isValidDecodeQualification(inputGRDFolder);

        isValidDecodeQualification(TestData.inputS1_SLC);

        isValidDecodeQualification(TestS1OCNInputProductValidator.inputS1_IW_metaOCN);
        isValidDecodeQualification(TestS1OCNInputProductValidator.inputS1_WV_metaOCN);

        isInValidDecodeQualification(TestSentinel1ETADProductReader.inputS1ETAD_IW);
        isInValidDecodeQualification(TestSentinel1ETADProductReader.inputS1ETAD_SM);
        isInValidDecodeQualification(TestSentinel1ETADProductReader.inputS1ETAD_SM_ZIP);
    }
}
