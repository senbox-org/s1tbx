package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataio.ProductIOException;

import java.io.IOException;

/**
 * EnvisatAuxReader Tester.
 *
 * @author lveci
 */
public class EnvisatAuxReaderTest extends TestCase {

    String ers1XCAFilePath = "org/esa/beam/resources/testdata/ER1_XCA_AXNXXX20050321_000000_19910101_000000_20100101_000000.txt";
    String ers2XCAFilePath = "org/esa/beam/resources/testdata/ER2_XCA_AXNXXX20050321_000000_19950101_000000_20100101_000000.txt";
    String envisatXCAFilePath = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000";
    String envisatXCAZipFilePath = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000.zip";
    String envisatXCAZipFilePath2 = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000_2";
    String envisatXCAGZFilePath = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000.gz";

    public EnvisatAuxReaderTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAutoLookupZIP() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAZipFilePath2);
        testAuxDataFromGADS(reader);
    }

    public void testUncompressed() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAFilePath);
        testAuxDataFromGADS(reader);
    }

    public void testERS1() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(ers1XCAFilePath);
    }

    public void testERS2() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(ers2XCAFilePath);
    }

    public void testGZIP() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAGZFilePath);
        testAuxDataFromGADS(reader);
    }

    public void testZIP() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAZipFilePath);
        testAuxDataFromGADS(reader);
    }

    static void testAuxDataFromGADS(EnvisatAuxReader reader) throws ProductIOException {
        ProductData extCalImVvData = reader.getAuxData("ext_cal_im_vv");
        assertNotNull(extCalImVvData);

        final float[] extCalImVv = ((float[]) extCalImVvData.getElems());

        assertEquals(7, extCalImVv.length);
        assertEquals(34994.515625f, extCalImVv[0], 1e-6f);
        assertEquals(32284.941406f, extCalImVv[1], 1e-5f);
        assertEquals(39084.089843f, extCalImVv[2], 1e-5f);
        assertEquals(33113.109375f, extCalImVv[3], 1e-5f);
        assertEquals(34994.516000f, extCalImVv[4], 1e-5f);
        assertEquals(34994.516000f, extCalImVv[5], 1e-5f);

        ProductData elevAngleData = reader.getAuxData("elev_ang_is1");
        float elevAngle1 = elevAngleData.getElemFloat();
        assertEquals(16.628, elevAngle1, 1e-5);

        ProductData patData = reader.getAuxData("pattern_is1");
        final float[] pattern1 = ((float[]) patData.getElems());

        assertEquals(804, pattern1.length);
        assertEquals(-18.6224f, pattern1[0], 1e-5f);
        assertEquals(-17.4271f, pattern1[1], 1e-5f);
        assertEquals(-16.3024f, pattern1[2], 1e-5f);
        assertEquals(-18.7799f, pattern1[804-3], 1e-5f);
        assertEquals(-19.5464f, pattern1[804-2], 1e-5f);
        assertEquals(-20.3164f, pattern1[804-1], 1e-5f);

        assertEquals("-18.6224", patData.getElemStringAt(0));
    }

}
