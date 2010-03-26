package org.esa.beam.dataio.spot;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class PhysVolDescriptorTest extends TestCase {

    public void testPhysVolumeDescriptor() throws IOException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");
        FileReader reader = new FileReader(file);
        try {
            PhysVolDescriptor descriptor = new PhysVolDescriptor(reader);
            assertEquals("0001", descriptor.getLogVolDirName());
            assertEquals("0001/0001_LOG.TXT", descriptor.getLogVolDescriptorFileName());
            assertEquals(1, descriptor.getPhysVolNumber());
            assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
            assertEquals("VGT PRODUCT FORMAT V1.5", descriptor.getFormatReference());
        } finally {
            reader.close();
        }
    }
}
