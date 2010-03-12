package org.esa.beam.dataio.spot;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.DecodeQualification;

import java.io.File;

public class SpotVgtProductReaderPlugInTest extends TestCase {

    public void testGetBandName() {
        assertEquals("VZA", SpotVgtProductReaderPlugIn.getBandName(new File("VZA.HDF")));
        assertEquals("VZA", SpotVgtProductReaderPlugIn.getBandName(new File("_VZA.HDF")));
        assertEquals("B2", SpotVgtProductReaderPlugIn.getBandName(new File("0001_B2.HDF")));
        assertEquals("MIR", SpotVgtProductReaderPlugIn.getBandName(new File("0001_MIR.HDF")));
    }

    public void testDecodeQualification() {
        SpotVgtProductReaderPlugIn plugIn = new SpotVgtProductReaderPlugIn();

        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(null));

        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./beam-spot-vgt-reader/src/test/data/");
            if (!dir.exists()) {
                fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }

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
}
