
package org.esa.s1tbx.io.risat1;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRisat1ProductReader extends ReaderTest {

    private final static String sep = S1TBXTests.sep;

    //private final static File inputCEOSZip = new File("D:\\EO\\RISAT\\SampleData\\CEOS\\FRS1_LEVEL1_GROUND_RANGE.zip");
    private final static File inputCEOSFolder = new File(TestData.inputSAR  + sep +  "RISAT1" + sep + "FRS-1" + sep + "9441sd1_s33_GroundRange");
    private final static File inputCEOSMetaXML = new File(TestData.inputSAR + sep + "RISAT1" + sep + "FRS-1" + sep + "9441sd1_s33_GroundRange" + sep + "BAND_META.txt");

    //private final static File inputGeoTiffFolder = new File("D:\\EO\\RISAT\\SampleData\\GeoTiff\\L2A_154074911_FRS1_Geotiff");

    public TestRisat1ProductReader() {
        super(new Risat1ProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputCEOSFolder + " not found", inputCEOSFolder.exists());
        assumeTrue(inputCEOSMetaXML + " not found", inputCEOSMetaXML.exists());
    }

    @Test
    public void testOpeningCEOSFolder() throws Exception {
        testReader(inputCEOSFolder);
    }

    @Test
    public void testOpeningCEOSMetadata() throws Exception {
        testReader(inputCEOSMetaXML);
    }

//    @Test
//    public void testGeoTiffFolder() throws Exception {
//        testReader(inputGeoTiffFolder);
//    }
}
