package org.esa.beam.dataio.spot;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoCoding;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogVolDescriptorTest extends TestCase {

    public void testIt() throws IOException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/0001/0001_LOG.TXT");
        FileReader reader = new FileReader(file);
        try {
            LogVolDescriptor descriptor = new LogVolDescriptor(reader);
            assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
            GeoCoding geoCoding = descriptor.getGeoCoding();
            assertNotNull(geoCoding);
        } finally {
            reader.close();
        }
    }
}
