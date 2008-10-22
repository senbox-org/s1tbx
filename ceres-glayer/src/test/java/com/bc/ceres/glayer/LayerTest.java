package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.support.BufferedImageRendering;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.bc.ceres.glayer.Assert2D.*;

public class LayerTest {

    @Test
    public void testDefaults() {
        final Layer layer = new Layer();
        assertEquals(layer.getClass().getName(), layer.getName());
        assertEquals(true, layer.isVisible());
        assertEquals(1.0, layer.getStyle().getOpacity(), 1e-10);
        assertEquals(Composite.SRC_OVER, layer.getStyle().getComposite());

        assertEquals(true, layer.getChildLayerList().isEmpty());
        assertNull(layer.getModelBounds());
    }

    @Test
    public void testPropertyAccess() {
        final Layer layer = new Layer();

        assertEquals(true, layer.isVisible());
        layer.setVisible(false);
        assertEquals(false, layer.isVisible());
        layer.setVisible(true);
        assertEquals(true, layer.isVisible());

        assertEquals(layer.getClass().getName(), layer.getName());
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

        assertEquals(1.0, layer.getStyle().getOpacity(), 1e-10);
        layer.getStyle().setOpacity(0.1);
        assertEquals(0.1, layer.getStyle().getOpacity(), 1e-10);
        layer.getStyle().setOpacity(1.0);
        assertEquals(1.0, layer.getStyle().getOpacity(), 1e-10);

        assertEquals(Composite.SRC_OVER, layer.getStyle().getComposite());
        layer.getStyle().setComposite(Composite.DST_OUT);
        assertEquals(Composite.DST_OUT, layer.getStyle().getComposite());
    }

    @Test
    public void testBounds() {
        final Layer layer = new Layer();
        layer.getChildLayerList().add(new ShapeLayer(new Shape[]{new Rectangle(-20, 10, 30, 50)}));
        layer.getChildLayerList().add(new ShapeLayer(new Shape[]{new Rectangle(-10, 20, 20, 60)}));
        layer.getChildLayerList().add(new ShapeLayer(new Shape[]{new Rectangle(0, 0, 40, 50)}));
        assertNotNull(layer.getModelBounds());
        assertEquals(new Rectangle(-20, 0, 60, 80), layer.getModelBounds());
    }

    @Test
    public void testRenderRecognisesVisibileState() {
        final Layer layer = new Layer();
        final RenderCountingLayer l1 = new RenderCountingLayer();
        final RenderCountingLayer l2 = new RenderCountingLayer();
        final RenderCountingLayer l3 = new RenderCountingLayer();

        layer.getChildLayerList().add(l1);
        layer.getChildLayerList().add(l2);
        layer.getChildLayerList().add(l3);

        final Rendering rendering = new BufferedImageRendering(16, 16);
        layer.render(rendering);
        assertEquals(1, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(1, l3.renderCount);

        l2.setVisible(false);
        layer.render(rendering);
        assertEquals(2, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(2, l3.renderCount);

        l3.setVisible(false);
        layer.render(rendering);
        assertEquals(3, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(2, l3.renderCount);

        layer.setVisible(false);
        layer.render(rendering);
        assertEquals(3, l1.renderCount);
        assertEquals(1, l2.renderCount);
        assertEquals(2, l3.renderCount);
    }

    @Test
    public void testPropertyChangeNotification() {
        final Layer layer = new Layer();
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

    @Test
    public void testListInterfaceImplementionSpecConformance() {

        final java.util.List<Layer> list = new Layer().getChildLayerList();

        assertEquals(true, list.isEmpty());
        assertEquals(0, list.size());

        Iterator<Layer> layerIterator = list.iterator();
        assertNotNull(layerIterator);
        assertEquals(false, layerIterator.hasNext());
        try {
            layerIterator.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }

        Layer layer0 = new Layer();
        list.add(layer0);

        assertEquals(false, list.isEmpty());
        assertEquals(1, list.size());

        layerIterator = list.iterator();
        assertNotNull(layerIterator);
        assertEquals(true, layerIterator.hasNext());
        assertSame(layer0, layerIterator.next());
        assertEquals(false, layerIterator.hasNext());
        try {
            layerIterator.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }

        assertSame(layer0, list.get(0));
        try {
            list.get(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        Layer layer1 = new Layer();
        list.add(layer1);

        assertEquals(2, list.size());
        assertSame(layer0, list.get(0));
        assertSame(layer1, list.get(1));

        layer0 = new Layer();
        list.set(0, layer0);
        assertSame(layer0, list.get(0));
        assertSame(layer1, list.get(1));

        list.remove(layer0);
        assertEquals(1, list.size());
        assertSame(layer1, list.get(0));

        list.remove(0);
        assertEquals(0, list.size());

        list.add(new Layer());
        list.add(new Layer());
        list.add(new Layer());
        final Iterator<Layer> iterator = list.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertEquals(0, list.size());


    }

    @Test
    public void testChildLayerListIsLife() {

        Layer owner = new Layer();
        final List<Layer> list = owner.getChildLayerList();


        final TracingLayerListener ll = new TracingLayerListener();
        owner.addListener(ll);

        list.add(new Layer());
        assertEquals("added 1;", ll.trace);

        list.add(new Layer());
        assertEquals("added 1;added 1;", ll.trace);

        assertSame(owner, list.get(0).getParentLayer());
        assertSame(owner, list.get(1).getParentLayer());

        Layer layer0 = list.remove(0);
        assertNull(layer0.getParentLayer());
        assertEquals("added 1;added 1;removed 1;", ll.trace);

        layer0 = list.set(0, new Layer());
        assertNull(layer0.getParentLayer());
        assertEquals("added 1;added 1;removed 1;removed 1;added 1;", ll.trace);
    }

    public static class RenderCountingLayer extends Layer {
        int renderCount;

        public Rectangle2D getModelBounds() {
            return null;
        }

        @Override
        protected void renderLayer(Rendering rendering) {
            renderCount++;
        }
    }
}