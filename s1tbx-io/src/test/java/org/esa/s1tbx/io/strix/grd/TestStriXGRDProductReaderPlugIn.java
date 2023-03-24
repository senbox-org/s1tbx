
package org.esa.s1tbx.io.strix.grd;

import org.esa.s1tbx.io.AbstractProductReaderPlugInTest;
import org.esa.snap.core.dataio.ProductReader;
import org.junit.Test;


import static org.esa.s1tbx.io.strix.grd.TestStriXGRDProductReader.inputSLGRDMeta;
import static org.esa.s1tbx.io.strix.grd.TestStriXGRDProductReader.inputSLGRDFolder;
import static org.esa.s1tbx.io.strix.grd.TestStriXGRDProductReader.inputSMGRDMeta;
import static org.esa.s1tbx.io.strix.grd.TestStriXGRDProductReader.inputSMGRDFolder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestStriXGRDProductReaderPlugIn extends AbstractProductReaderPlugInTest {

    public TestStriXGRDProductReaderPlugIn() {
        super(new StriXGRDProductReaderPlugIn());
    }

    @Test
    public void testCreateReaderInstance() {
        ProductReader productReader = plugin.createReaderInstance();
        assertNotNull(productReader);
        assertTrue(productReader instanceof StriXGRDProductReader);
    }

    @Test
    public void testGetFormatNames() {
        assertArrayEquals(new String[]{"StriX GRD"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() {
        assertArrayEquals(new String[]{".xml"}, plugin.getDefaultFileExtensions());
    }

    @Override
    protected String[] getValidPrimaryMetadataFileNames() {
        return new String[] {"par-.xml", "PAR-.xml"};
    }

    @Override
    protected String[] getInvalidPrimaryMetadataFileNames() {
        return new String[] {"PAR-xyz.json"};
    }

    @Test
    public void testValidDecodeQualification() {
        isValidDecodeQualification(inputSMGRDMeta);
        isValidDecodeQualification(inputSMGRDFolder);

        isValidDecodeQualification(inputSLGRDMeta);
        isValidDecodeQualification(inputSLGRDFolder);
    }
}
