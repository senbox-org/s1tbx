package org.esa.snap.dataio.netcdf.util;

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

    @Test
    public void testGetDimensionX() throws Exception {
        final Dimension yDim = new Dimension("y", 256);
        final Dimension xDim = new Dimension("x", 512);

        assertSame(xDim, new DimKey(yDim, xDim).getDimensionX());
    }

    @Test
    public void testGetDimensionsForSynVgtProducts() throws Exception {
        final Dimension yDim = new Dimension("NY", 2800);
        final Dimension xDim = new Dimension("NX", 4032);
        final Dimension vDim = new Dimension("nv", 2);

        final DimKey dimKey = new DimKey(yDim, xDim, vDim);

        assertSame(xDim, dimKey.getDimensionX());
        assertSame(yDim, dimKey.getDimensionY());
    }
}
