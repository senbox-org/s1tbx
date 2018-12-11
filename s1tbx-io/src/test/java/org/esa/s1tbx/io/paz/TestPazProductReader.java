
package org.esa.s1tbx.io.paz;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestPazProductReader extends ReaderTest {

    private final static File inputMetaXML = new File("D:\\EO\\PAZ\\PAZ1_SAR__SSC______SM_S_SRA_20110312T204307_20110312T204315\\PAZ1_SAR__SSC______SM_S_SRA_20110312T204307_20110312T204315.xml");

    public TestPazProductReader() {
        super(new PazProductReaderPlugIn());
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
