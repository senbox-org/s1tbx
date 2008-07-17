package com.bc.layer;

import com.bc.ImagingTestCase;


public class AbstractGraphicalLayerTest extends ImagingTestCase {

    public void testInheritedProperties() {
        final TestLayer layer = new TestLayer();

        assertNotNull(layer.getBoundingBox());
        assertTrue(layer.getBoundingBox().isEmpty());

        assertEquals(true, layer.isVisible());
        layer.setVisible(false);
        assertEquals(false, layer.isVisible());
        layer.setVisible(true);
        assertEquals(true, layer.isVisible());

        assertEquals("com.bc.layer.TestLayer", layer.getName());
        layer.setName("Grid");
        assertEquals("Grid", layer.getName());
        layer.setName("Earth grid");
        assertEquals("Earth grid", layer.getName());
        try {
            layer.setName(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }

        assertEquals(0.0f, layer.getTransparency());
        layer.setTransparency(0.1f);
        assertEquals(0.1f, layer.getTransparency());
        layer.setTransparency(1.0f);
        assertEquals(1.0f, layer.getTransparency());
        try {
            layer.setTransparency(-0.1f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            layer.setTransparency(1.1f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertEquals(AlphaCompositeMode.SRC_OVER, layer.getAlphaCompositeMode());
        layer.setAlphaCompositeMode(AlphaCompositeMode.DST_OUT);
        assertEquals(AlphaCompositeMode.DST_OUT, layer.getAlphaCompositeMode());
        try {
            layer.setAlphaCompositeMode(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testPropertyChangeListeners() {
        final TestLayer layer = new TestLayer();
        final TracingPCL pcl = new TracingPCL();
        layer.addPropertyChangeListener(pcl);

        layer.setName("Grid");
        layer.setVisible(false);
        layer.setTransparency(0.5f);
        assertEquals("name;visible;transparency;", pcl.trace);

        layer.setName("Grid");
        layer.setVisible(false);
        layer.setTransparency(1.0f);
        assertEquals("name;visible;transparency;transparency;", pcl.trace);

        layer.setTransparency(1.0f);
        layer.setVisible(true);
        layer.setName("Raster");
        assertEquals("name;visible;transparency;transparency;visible;name;", pcl.trace);

        layer.setAlphaCompositeMode(AlphaCompositeMode.DST_IN);
        assertEquals("name;visible;transparency;transparency;visible;name;alphaCompositeMode;", pcl.trace);

        pcl.trace = "";
        layer.removePropertyChangeListener(pcl);

        layer.setTransparency(0.25f);
        layer.setVisible(false);
        layer.setName("Graticule");
        layer.setAlphaCompositeMode(AlphaCompositeMode.SRC_OUT);
        assertEquals("", pcl.trace);

    }

}