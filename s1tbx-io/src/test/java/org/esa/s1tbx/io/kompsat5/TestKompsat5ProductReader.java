
package org.esa.s1tbx.io.kompsat5;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.junit.Test;

import java.io.File;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestKompsat5ProductReader extends ReaderTest {

    private final static File inputZip = null;
    private final static File inputFolder = new File("E:\\data\\K5\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D");
    private final static File inputMetaXML = new File("E:\\data\\K5\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D_Aux.xml");

    public TestKompsat5ProductReader() {
        super(new Kompsat5ReaderPlugIn());
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
