package org.esa.beam.dataio.spot;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LogVolDescriptorTest extends TestCase {

    public void testIt() throws IOException,ParseException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/0001/0001_LOG.TXT");
        FileReader reader = new FileReader(file);
        try {
            LogVolDescriptor descriptor = new LogVolDescriptor(reader);
            assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
            assertNotNull(descriptor.getGeoCoding());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
            assertEquals(dateFormat.parse("20060720223132"), descriptor.getStartDate());
            assertEquals(dateFormat.parse("20060730235628"), descriptor.getEndDate());
        } finally {
            reader.close();
        }
    }
}
