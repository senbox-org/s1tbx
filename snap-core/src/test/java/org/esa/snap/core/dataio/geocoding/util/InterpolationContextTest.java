package org.esa.snap.core.dataio.geocoding.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InterpolationContextTest {

    @Test
    public void testConstruction() {
        final InterpolationContext context = new InterpolationContext();
        assertEquals(4, context.lons.length);
        assertEquals(4, context.lats.length);
        assertEquals(4, context.x.length);
        assertEquals(4, context.y.length);
    }

    @Test
    public void testExtract() {
        final double[] longitudes = {
                12.0, 13.0, 14.0, 15.0,
                12.1, 13.1, 14.1, 15.1,
                12.2, 13.2, 14.2, 15.2};
        final double[] latitudes = {
                23.0, 24.0, 25.0, 26.0,
                23.2, 24.2, 25.2, 26.2,
                23.4, 24.4, 25.4, 26.4};
        final int rasterWidth = 4;
        final int rasterHeight = 3;

        InterpolationContext context = InterpolationContext.extract(1, 1, longitudes, latitudes, rasterWidth, rasterHeight);
        assertEquals(13.0, context.lons[0], 1e-8);
        assertEquals(25.0, context.lats[1], 1e-8);
        assertEquals(1, context.x[2]);
        assertEquals(1, context.y[3]);

        context = InterpolationContext.extract(3, 1, longitudes, latitudes, rasterWidth, rasterHeight);
        assertEquals(14.1, context.lons[0], 1e-8);
        assertEquals(26.2, context.lats[1], 1e-8);
        assertEquals(2, context.x[2]);
        assertEquals(2, context.y[3]);
    }

    @Test
    public void testExtract_borderPixel() {
        final double[] longitudes = {
                13.0, 14.0, 15.0, 16.0,
                13.1, 14.1, 15.1, 16.1,
                13.2, 14.2, 15.2, 16.2};
        final double[] latitudes = {
                24.0, 25.0, 26.0, 27.0,
                24.2, 25.2, 26.2, 27.2,
                24.4, 25.4, 26.4, 27.4};
        final int rasterWidth = 4;
        final int rasterHeight = 3;

        // upper left
        InterpolationContext context = InterpolationContext.extract(0, 0, longitudes, latitudes, rasterWidth, rasterHeight);
        assertEquals(13.0, context.lons[0], 1e-8);
        assertEquals(25.0, context.lats[1], 1e-8);
        assertEquals(0, context.x[2]);
        assertEquals(1, context.y[3]);

        // upper right
        context = InterpolationContext.extract(3, 0, longitudes, latitudes, rasterWidth, rasterHeight);
        assertEquals(15.0, context.lons[0], 1e-8);
        assertEquals(27.0, context.lats[1], 1e-8);
        assertEquals(2, context.x[2]);
        assertEquals(1, context.y[3]);

        // lower right
        context = InterpolationContext.extract(3, 2, longitudes, latitudes, rasterWidth, rasterHeight);
        assertEquals(15.1, context.lons[0], 1e-8);
        assertEquals(27.2, context.lats[1], 1e-8);
        assertEquals(2, context.x[2]);
        assertEquals(2, context.y[3]);

        // lower left
        context = InterpolationContext.extract(0, 2, longitudes, latitudes, rasterWidth, rasterHeight);
        assertEquals(13.1, context.lons[0], 1e-8);
        assertEquals(25.2, context.lats[1], 1e-8);
        assertEquals(0, context.x[2]);
        assertEquals(2, context.y[3]);
    }
}
