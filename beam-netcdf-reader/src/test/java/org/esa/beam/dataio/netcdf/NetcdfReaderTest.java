package org.esa.beam.dataio.netcdf;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Calendar;

/**
 * Tests class {@link NetcdfReader}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NetcdfReaderTest extends TestCase {

    public void testGlobalAttributes() throws IOException {

        final URL url = NetcdfReaderTest.class.getResource("test.nc");
        assertNotNull(url);
        assertEquals("file", url.getProtocol());

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith("test.nc"));

        final File file = new File(path);
        assertEquals(file.getName(), "test.nc");
        assertTrue(file.exists());
        assertTrue(file.canRead());

        final ProductReader reader = new NetcdfReaderPlugIn().createReaderInstance();
        final Product product = reader.readProductNodes(file.getPath(), null);
        assertNotNull(product);

        testStartTime(product);
        testEndTime(product);
    }

    private void testStartTime(final Product product) {
        final ProductData.UTC utc = product.getStartTime();
        assertNotNull(utc);

        final Calendar startTime = utc.getAsCalendar();
        assertEquals(2002, startTime.get(Calendar.YEAR));
        assertEquals(11, startTime.get(Calendar.MONTH));
        assertEquals(24, startTime.get(Calendar.DATE));
        assertEquals(11, startTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(12, startTime.get(Calendar.MINUTE));
        assertEquals(13, startTime.get(Calendar.SECOND));
    }

    private void testEndTime(final Product product) {
        final ProductData.UTC utc = product.getEndTime();
        assertNotNull(utc);

        final Calendar endTime = utc.getAsCalendar();
        assertEquals(2002, endTime.get(Calendar.YEAR));
        assertEquals(11, endTime.get(Calendar.MONTH));
        assertEquals(24, endTime.get(Calendar.DATE));
        assertEquals(11, endTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(12, endTime.get(Calendar.MINUTE));
        assertEquals(14, endTime.get(Calendar.SECOND));
    }
}
