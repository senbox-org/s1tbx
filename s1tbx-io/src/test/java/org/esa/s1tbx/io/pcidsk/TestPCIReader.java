
package org.esa.s1tbx.io.pcidsk;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test PCI Product Reader.
 *
 * @author lveci
 */
public class TestPCIReader extends ReaderTest {

    private final static String sep = S1TBXTests.sep;
    private final static File file = new File(S1TBXTests.inputPathProperty + sep +  "SAR" + sep + "pcidsk\\kompsat2_pcidsk_msc.pix");

    @Before
    public void setup() {
        assumeTrue(file.exists());
    }

    public TestPCIReader() {
        super(new PCIReaderPlugIn());
    }

    @Test
    public void testOpeningFile() throws Exception {
        verifyTime = false;
        testReader(file);
    }
}