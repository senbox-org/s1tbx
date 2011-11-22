package org.esa.beam.dataio.netcdf.util;

import org.junit.Test;
import ucar.nc2.Dimension;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class DimKeyTest {
    @Test
    public void testEqualsAndHashCode() throws Exception {
        DimKey dimKey1 = new DimKey(new Dimension("y", 256), new Dimension("x", 512));
        DimKey dimKey2 = new DimKey(new Dimension("y", 256), new Dimension("x", 512));
        assertTrue(dimKey1.equals(dimKey2));
        assertTrue(dimKey2.equals(dimKey1));
        assertTrue(dimKey1.hashCode() == dimKey2.hashCode());

        dimKey2 = new DimKey(new Dimension("t", 12), new Dimension("y", 256), new Dimension("x", 512));
        assertTrue(dimKey1.equals(dimKey2));
        assertTrue(dimKey2.equals(dimKey1));
        assertTrue(dimKey1.hashCode() == dimKey2.hashCode());

        dimKey2 = new DimKey(new Dimension("t", 12), new Dimension("y", 256), new Dimension("x", 256));
        assertFalse(dimKey1.equals(dimKey2));
        assertFalse(dimKey2.equals(dimKey1));
        assertFalse(dimKey1.hashCode() == dimKey2.hashCode());
    }

}
