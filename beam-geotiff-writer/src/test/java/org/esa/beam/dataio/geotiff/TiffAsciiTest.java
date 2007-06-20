package org.esa.beam.dataio.geotiff;

import junit.framework.TestCase;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * TiffAscii Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/21/2005</pre>
 */

public class TiffAsciiTest extends TestCase {

    public void testCreation() {
        new TiffAscii("Alois und Sepp");
    }

    public void testCreation_null() {
        try {
            final String value = null;
            new TiffAscii(value);
            fail("IllegalArgumentException expected because value is null");
        } catch (IllegalArgumentException expected) {
            // expected Exception
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected: but was [" + notExpected.getClass().getName() + "]");
        }
    }

    public void testCreation_EmptyString() {
        try {
            final String value = "";
            new TiffAscii(value);
            fail("IllegalArgumentException expected because value is an empty string");
        } catch (IllegalArgumentException expected) {
            // expected Exception
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected: but was [" + notExpected.getClass().getName() + "]");
        }
    }

    public void testGetValue() throws Exception {
        final TiffAscii tiffAscii = new TiffAscii("Alois und Sepp");
        assertEquals("Alois und Sepp", tiffAscii.getValue());
    }

    public void testWrite() throws Exception {
        final TiffAscii tiffAscii = new TiffAscii("Alois und Sepp");

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(os);
        tiffAscii.write(ios);
        ios.flush();
        assertEquals("Alois und Sepp|", os.toString());
    }

    public void testGetSizeInBytes() {
        final String value = "Hedi und Fredi";
        final TiffAscii tiffAscii = new TiffAscii(value);
        assertEquals(value.length() + 1, tiffAscii.getSizeInBytes());
    }
}
