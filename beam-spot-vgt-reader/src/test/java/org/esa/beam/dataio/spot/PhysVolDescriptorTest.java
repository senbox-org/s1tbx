package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class PhysVolDescriptorTest extends TestCase {

    public void testPhysVolumeDescriptor() throws IOException {
        File dir = SpotVgtProductReaderPlugInTest.getTestDataDir();
        File file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");
        PhysVolDescriptor descriptor = new PhysVolDescriptor(file);
        assertSame(file, descriptor.getFile());
        assertEquals(new File(dir, "decode_qual_intended/0001"), descriptor.getDataDir());
        assertEquals(1, descriptor.getPhysVolNumber());
        assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
        assertEquals("VGT PRODUCT FORMAT V1.5", descriptor.getFormatReference());
    }
}
