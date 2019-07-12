
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

    //private final static File inputMetaXML = new File("D:\\EO\\PAZ\\PAZ1_SAR__SSC______SM_S_SRA_20110312T204307_20110312T204315\\PAZ1_SAR__SSC______SM_S_SRA_20110312T204307_20110312T204315.xml");
    private final static File inputMetaXML = new File(S1TBXTests.inputPathProperty, "SAR\\PAZ\\PAZ1_SAR__SSC______SM_S_SRA_20110312T204307_20110312T204315\\PAZ1_SAR__SSC______SM_S_SRA_20110312T204307_20110312T204315.xml");

    public TestPazProductReader() {
        super(new PazProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputMetaXML + " not found", inputMetaXML.exists());
    }

    @Test
    @Ignore
    public void testOpeningFolder() throws Exception {
        //testReader(inputFolder);
    }

    @Test
    public void testOpeningMetadata() throws Exception {
        testReader(inputMetaXML);
    }

    @Test
    @Ignore
    public void testOpeningZip() throws Exception {
        //testReader(inputZip);
    }
}
