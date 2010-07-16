package org.esa.beam.dataio.netcdf;

import org.junit.Test;

/**
 * User: Thomas Storm
 * Date: 26.03.2010
 * Time: 14:01:42
 */
public class Nc4BeamWriterTest {

    @Test
    public void testWriting() {
        final NetCdfWriter writer = (NetCdfWriter) new NetCdfWriterPlugIn().createWriterInstance();
    }
}
