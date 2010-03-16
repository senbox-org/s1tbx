package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;

public class SpotVgtProductReaderPlugInTest extends TestCase {

    public void testGetBandName() {
        assertEquals("VZA", SpotVgtProductReaderPlugIn.getBandName(new File("VZA.HDF")));
        assertEquals("VZA", SpotVgtProductReaderPlugIn.getBandName(new File("_VZA.HDF")));
        assertEquals("B2", SpotVgtProductReaderPlugIn.getBandName(new File("0001_B2.HDF")));
        assertEquals("MIR", SpotVgtProductReaderPlugIn.getBandName(new File("0001_MIR.HDF")));
    }

    public void testPhysVolumeDescriptor() throws IOException {
        File dir = getTestDataDir();
        File file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");

        PropertySet physVolDescriptor = SpotVgtProductReaderPlugIn.readPhysVolDescriptor(file);
        assertNotNull(physVolDescriptor);
        Property[] properties = physVolDescriptor.getProperties();
        assertEquals(24, properties.length);
        assertEquals("1", physVolDescriptor.getValue("PHYS_VOL_NUMBER"));
        assertEquals("V2KRNS10__20060721E", physVolDescriptor.getValue("PRODUCT_#0001_ID"));
        assertEquals("_MIR.HDF", physVolDescriptor.getValue("PRODUCT_#0001_PLAN_09"));
    }

    public void testDecodeQualification() {
        SpotVgtProductReaderPlugIn plugIn = new SpotVgtProductReaderPlugIn();

        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(null));

        File dir = getTestDataDir();
        File file;

        file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_suitable/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.SUITABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.SUITABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_1/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_2/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_3/TEST.TXT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_3/NON_EXISTENT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));
    }

    static File getTestDataDir() {
        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./beam-spot-vgt-reader/src/test/data/");
            if (!dir.exists()) {
                fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    public static void main(String[] args) throws IOException {
        Product product = ProductIO.readProduct(new File(args[0]));
        System.out.println("product = " + product);
        product.closeIO();
    }
}
