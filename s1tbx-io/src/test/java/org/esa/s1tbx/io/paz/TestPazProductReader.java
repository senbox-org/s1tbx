
package org.esa.s1tbx.io.paz;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestPazProductReader extends ReaderTest {

    private final static String sep = S1TBXTests.sep;

    private final static File inputMGDMetaXML = new File(S1TBXTests.inputPathProperty + sep +  "SAR" + sep + "PAZ" + sep + "NewDelhi\\PAZ1_SAR__MGD_RE___SC_S_SRA_20180616T004650_20180616T004712\\PAZ1_SAR__MGD_RE___SC_S_SRA_20180616T004650_20180616T004712.xml");
    private final static File inputMGDFolder = new File(S1TBXTests.inputPathProperty + sep +  "SAR" + sep + "PAZ" + sep + "NewDelhi\\PAZ1_SAR__MGD_RE___SC_S_SRA_20180616T004650_20180616T004712");

    private final static File inputSSCMetaXML = new File(S1TBXTests.inputPathProperty + sep +  "SAR" + sep + "PAZ" + sep + "Mojave Interferometric pair\\PAZ1_SAR__SSC______SM_S_SRA_20180520T014220_20180520T014228\\PAZ1_SAR__SSC______SM_S_SRA_20180520T014220_20180520T014228.xml");
    private final static File inputSSCFolder = new File(S1TBXTests.inputPathProperty + sep +  "SAR" + sep + "PAZ" + sep + "Mojave Interferometric pair\\PAZ1_SAR__SSC______SM_S_SRA_20180520T014220_20180520T014228");

    public TestPazProductReader() {
        super(new PazProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputMGDMetaXML + " not found", inputMGDMetaXML.exists());
        assumeTrue(inputMGDFolder + " not found", inputMGDFolder.exists());
        assumeTrue(inputSSCMetaXML + " not found", inputSSCMetaXML.exists());
        assumeTrue(inputSSCFolder + " not found", inputSSCFolder.exists());
    }

//    @Test
//    public void testOpeningMGDFolder() throws Exception {
//        testReader(inputMGDFolder);
//    }

    @Test
    public void testOpeningMGDMetadata() throws Exception {
        testReader(inputMGDMetaXML);
    }

//    @Test
//    public void testOpeningSSCFolder() throws Exception {
//        testReader(inputSSCFolder);
//    }

    @Test
    public void testOpeningSSCMetadata() throws Exception {
        testReader(inputSSCMetaXML);
    }

//    @Test
//    @Ignore
//    public void testOpeningZip() throws Exception {
//        //testReader(inputZip);
//    }
}
