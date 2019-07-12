
package org.esa.s1tbx.io.kompsat5;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestKompsat5ProductReader extends ReaderTest {

    private final static File inputZip = null;
    private static final File inputFolder = new File(S1TBXTests.inputPathProperty, "SAR\\K5\\HDF\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D");
    //private final static File inputFolder = new File("E:\\data\\K5\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D");
    private final static File inputMetaXML = new File(S1TBXTests.inputPathProperty, "SAR\\K5\\HDF\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D_Aux.xml");
    //private final static File inputMetaXML = new File("E:\\data\\K5\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D_Aux.xml");

    public TestKompsat5ProductReader() {
        super(new Kompsat5ReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputFolder + " not found", inputFolder.exists());
        assumeTrue(inputMetaXML + " not found", inputMetaXML.exists());
    }

    @Test
    public void testOpeningFolder() throws Exception {
        testReader(inputFolder);
    }

    @Test
    public void testOpeningMetadata() throws Exception {
        testReader(inputMetaXML);
    }

//    @Test
//    public void testOpeningZip() throws Exception {
//        testReader(inputZip);
//    }
}
