
package org.csa.rstb.io.radarsat1;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Radarsat 1 CEOS Product Reader.
 *
 * @author lveci
 */
public class TestRadarsatProductReader extends ReaderTest  {

    private static final File zipFile = new File(S1TBXTests.TEST_ROOT + "RS1/RS1_m0700850_S7_20121103_232202_HH_SGF.zip");
    private static final File folder = new File(S1TBXTests.TEST_ROOT + "RS1/RS1_m0700850_S7_20121103_232202_HH_SGF");
    private static final File metaFile = new File(S1TBXTests.TEST_ROOT + "RS1/RS1_m0700850_S7_20121103_232202_HH_SGF/RS1_m0700850_S7_20121103_232202_HH_SGF.vol");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(zipFile + " not found", zipFile.exists());
        assumeTrue(folder + " not found", folder.exists());
        assumeTrue(metaFile + " not found", metaFile.exists());
    }

    public TestRadarsatProductReader() {
        super(new RadarsatProductReaderPlugIn());
    }

    @Test
    public void testOpeningZip() throws Exception {
        Product prod = testReader(zipFile.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH"});
    }

//    @Test
//    public void testOpeningFolder() throws Exception {
//        testReader(folder);
//    }

    @Test
    public void testOpeningVolFile() throws Exception {
        Product prod = testReader(metaFile.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH"});
    }
}
