package org.esa.beam.processor.binning;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.GeoPos;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 18.11.2005
 * Time: 12:31:04
 * To change this template use File | Settings | File Templates.
 */
public class L3ProjectionRasterTest extends TestCase {
    private L3ProjectionRaster projectionRaster;

    public void testGetWidth() {
        assertEquals("Width", 2220, projectionRaster.getWidth());
    }

    public void testGetHeight() {
        assertEquals("Height", 1110, projectionRaster.getHeight());
    }

    public void setUp() {
        projectionRaster = new L3ProjectionRaster();
        projectionRaster.init(111, new GeoPos(20, 10), new GeoPos(20, 30), new GeoPos(10, 30), new GeoPos(10, 10));
    }
}
