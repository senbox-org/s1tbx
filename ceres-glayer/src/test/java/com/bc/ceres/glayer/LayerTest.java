package com.bc.ceres.glayer;

public class LayerTest extends ImagingTestCase {

    public void testInheritedProperties() {
        final TestLayer layer = new TestLayer();

        assertEquals(true, layer.isVisible());
        layer.setVisible(false);
        assertEquals(false, layer.isVisible());
        layer.setVisible(true);
        assertEquals(true, layer.isVisible());

        assertEquals("com.bc.ceres.glayer.TestLayer", layer.getName());
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

        assertEquals(1.0, layer.getStyle().getOpacity());
        layer.getStyle().setOpacity(0.1);
        assertEquals(0.1, layer.getStyle().getOpacity());
        layer.getStyle().setOpacity(1.0);
        assertEquals(1.0, layer.getStyle().getOpacity());

        assertEquals(Composite.SRC_OVER, layer.getStyle().getComposite());
        layer.getStyle().setComposite(Composite.DST_OUT);
        assertEquals(Composite.DST_OUT, layer.getStyle().getComposite());
    }

    public void testPropertyChangeListeners() {
        final TestLayer layer = new TestLayer();
        final TracingLayerListener ll = new TracingLayerListener();
        layer.addListener(ll);

        layer.setName("Grid");
        layer.setVisible(false);
        layer.getStyle().setOpacity(0.5);
        assertEquals("name;visible;opacity;", ll.trace);

        layer.setName("Grid");
        layer.setVisible(false);
        layer.getStyle().setOpacity(0.0);
        assertEquals("name;visible;opacity;opacity;", ll.trace);

        layer.getStyle().setOpacity(0.0);
        layer.setVisible(true);
        layer.setName("Raster");
        assertEquals("name;visible;opacity;opacity;visible;name;", ll.trace);

        layer.getStyle().setComposite(Composite.DST_IN);
        assertEquals("name;visible;opacity;opacity;visible;name;composite;", ll.trace);

        ll.trace = "";
        layer.removeListener(ll);

        layer.getStyle().setOpacity(0.25);
        layer.setVisible(false);
        layer.setName("Graticule");
        layer.getStyle().setComposite(Composite.SRC_OUT);
        assertEquals("", ll.trace);

    }

}