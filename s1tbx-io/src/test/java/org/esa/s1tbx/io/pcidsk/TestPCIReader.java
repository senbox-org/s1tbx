
package org.esa.s1tbx.io.pcidsk;

import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Test;

import java.io.File;

/**
 * Test PCI Product Reader.
 *
 * @author lveci
 */
public class TestPCIReader {

    private static final String filePath = "P:\\rstb\\rstb\\data\\PCIDSK";

    private final PCIReaderPlugIn readerPlugin = new PCIReaderPlugIn();
    private final ProductReader reader = readerPlugin.createReaderInstance();

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        final File folder = new File(filePath);
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, new File[]{folder}, readerPlugin, reader, null, null);
    }
}