package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * EnvisatAuxReader Tester.
 *
 * @author lveci
 */
public class TestEnvisatAuxReader extends TestCase {

    String envisatXCAFilePath = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000";
    String envisatXCAZipFilePath = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000.zip";
    String envisatXCAGZFilePath = "org/esa/beam/resources/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000.gz";

    public TestEnvisatAuxReader(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOpenFile() {

        EnvisatAuxReader reader = new EnvisatAuxReader();

        try {
            reader.readProduct(envisatXCAFilePath);

            getAuxDataFromGADS(reader);

        } catch (IOException e) {

        }
    }

    public void testZipFiles() {

        EnvisatAuxReader reader = new EnvisatAuxReader();

        try {
            reader.readProduct(envisatXCAZipFilePath);

            reader.readProduct(envisatXCAGZFilePath);

        } catch (IOException e) {

        }
    }

    static void getAuxDataFromGADS(EnvisatAuxReader reader) {

        try {
            ProductData cal_im_vv_data = reader.getAuxData("ext_cal_im_vv");
            final float[] floats = ((float[]) cal_im_vv_data.getElems());

            for (float val : floats) {
                System.out.print(val + ", ");
            }
            System.out.println();

            ProductData elevAngleData = reader.getAuxData("elev_ang_is1");
            float elevAngle1 = elevAngleData.getElemFloat();
            System.out.print("elevation angle: " + elevAngle1);
            System.out.println();
            //assertEquals(elevAngle1, 16.628);

            ProductData patData = reader.getAuxData("pattern_is1");
            final float[] pattern1 = ((float[]) patData.getElems());

            System.out.print("num values " + pattern1.length);
            System.out.println();

            //for (float val : pattern1) {
            //    System.out.print(val + ", ");
            //}
            System.out.println();

            String num = patData.getElemStringAt(0);
            System.out.print(num);

        } catch (ProductIOException e) {

        }
    }

}
