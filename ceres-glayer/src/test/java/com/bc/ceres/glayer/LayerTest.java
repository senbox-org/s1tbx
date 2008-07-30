package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.support.BufferedImageRendering;

import java.awt.*;

public class LayerTest extends ImagingTestCase {

    public void testConstructor() {
        final Layer cl = new Layer();
        assertEquals(true, cl.getChildLayers().isEmpty());
        assertEquals(0, cl.getChildLayers().size());
        assertNull(cl.getBounds());
    }

    public void testBounds() {
        final Layer cl = new Layer();
        cl.getChildLayers().add(new ShapeLayer(new Shape[]{new Rectangle(-20, 10, 30, 50)}));
        cl.getChildLayers().add(new ShapeLayer(new Shape[]{new Rectangle(-10, 20, 20, 60)}));
        cl.getChildLayers().add(new ShapeLayer(new Shape[]{new Rectangle(0, 0, 40, 50)}));
        assertNotNull(cl.getBounds());
        assertEquals(new Rectangle(-20, 0, 60, 80), cl.getBounds());
    }

    public void testRenderRecognisesVisibileState() {
        final Layer cl = new Layer();
        final TestLayer l1 = new TestLayer();
        final TestLayer l2 = new TestLayer();
        final TestLayer l3 = new TestLayer();

        cl.getChildLayers().add(l1);
        cl.getChildLayers().add(l2);
        cl.getChildLayers().add(l3);

        final Rendering rendering = new BufferedImageRendering(16, 16);
        cl.render(rendering);
        assertEquals(1, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(1, l3.renderCount);

        l2.setVisible(false);
        cl.render(rendering);
        assertEquals(2, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(2, l3.renderCount);

        l3.setVisible(false);
        cl.render(rendering);
        assertEquals(3, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(2, l3.renderCount);

        cl.setVisible(false);
        cl.render(rendering);
        assertEquals(3, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(2, l3.renderCount);
    }


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

    public void testListeners() {
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