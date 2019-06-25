
package org.esa.s1tbx.io.risat1;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.TestData;
import org.junit.Test;

import java.io.File;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRisat1ProductReader extends ReaderTest {

    private final static File inputCEOSZip = new File("D:\\EO\\RISAT\\SampleData\\CEOS\\FRS1_LEVEL1_GROUND_RANGE.zip");
    private final static File inputCEOSFolder = new File(TestData.inputSAR + "RISAT1/FRS-1/9441sd1_s33_GroundRange");
    private final static File inputCEOSMetaXML = new File(TestData.inputSAR + "RISAT1/FRS-1/9441sd1_s33_GroundRange/BAND_META.txt");

    private final static File inputGeoTiffFolder = new File("D:\\EO\\RISAT\\SampleData\\GeoTiff\\L2A_154074911_FRS1_Geotiff");

    public TestRisat1ProductReader() {
        super(new Risat1ProductReaderPlugIn());
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
