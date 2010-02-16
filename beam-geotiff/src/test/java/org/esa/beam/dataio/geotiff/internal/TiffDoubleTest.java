package org.esa.beam.dataio.geotiff.internal;

import junit.framework.TestCase;

/**
 * TiffDouble Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/21/2005</pre>
 */

public class TiffDoubleTest extends TestCase {

    public void testCreation() {
        new TiffDouble(123497d);
    }

    public void testGetValue() throws Exception {
        final TiffDouble tiffDouble = new TiffDouble(123497d);
        assertEquals(123497d, tiffDouble.getValue(), 1e-10);
    }

    public void testGetSizeInBytes() {
        final TiffDouble tiffDouble = new TiffDouble(932846d);
        assertEquals(8, tiffDouble.getSizeInBytes());
    }
}
