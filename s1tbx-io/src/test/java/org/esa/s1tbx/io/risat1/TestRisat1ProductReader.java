
package org.esa.s1tbx.io.risat1;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.junit.Test;

import java.io.File;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRisat1ProductReader extends ReaderTest {

    private final static File inputZip = new File("D:\\EO\\RISAT\\SampleData\\FRS1_LEVEL1_GROUND_RANGE.zip");
    private final static File inputFolder = new File("D:\\EO\\RISAT\\SampleData\\FRS1_LEVEL1_GROUND_RANGE");
    private final static File inputMetaXML = new File("D:\\EO\\RISAT\\SampleData\\FRS1_LEVEL1_GROUND_RANGE\\BAND_META.txt");

    public TestRisat1ProductReader() {
        super(new Risat1ProductReaderPlugIn());
    }

    @Test
    public void testOpeningFolder() throws Exception {
        testReader(inputFolder);
    }

    @Test
    public void testOpeningMetadata() throws Exception {
        testReader(inputMetaXML);
    }

    @Test
    public void testOpeningZip() throws Exception {
        testReader(inputZip);
    }
}
