package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.support.BufferedImageRendering;
import static org.junit.Assert.*;
import static com.bc.ceres.glayer.Assert2D.*;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class LayerTest {
    @Test
    public void testType() {
        Layer layer1 = new Layer();
        Layer layer2 = new Layer();

        assertNotNull(layer1.getLayerType());
        assertSame(layer1.getLayerType(), layer2.getLayerType());
        assertSame(layer1.getLayerType(), LayerType.getLayerType(Layer.Type.class.getName()));
    }

    @Test
    public void testDefaults() {
        Layer layer = new Layer();
        LayerType layerType = layer.getLayerType();
        assertNotNull(layerType);
        assertTrue(layerType.createLayer(null, null) instanceof Layer);
        assertTrue(layerType.isValidFor(null));
        assertNotNull(layer.getId());
        assertEquals(layer.getClass().getName(), layer.getName());
        assertEquals(true, layer.isVisible());
        assertEquals(1.0, layer.getStyle().getOpacity(), 1e-10);
        assertEquals(Composite.SRC_OVER, layer.getStyle().getComposite());

        assertNull(layer.getModelBounds());
    }

    @Test
    public void testPropertyAccess() {
        Layer layer = new Layer();

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
    public void testModelBounds() {
        Layer layer;
        int x1, x2, y1, y2;

        layer = new Layer();
        assertEquals(null, layer.getModelBounds());

        layer = new Layer();
        layer.getChildren().add(new Layer());
        layer.getChildren().add(new Layer());
        layer.getChildren().add(new Layer());
        assertEquals(null, layer.getModelBounds());

        layer = new Layer();
        layer.getChildren().add(new Layer());
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(20, 10, 30, 50)}));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(10, 20, 20, 60)}));
        x1 = Math.min(20, 10);
        y1 = Math.min(10, 20);
        x2 = Math.max(20 + 30, 10 + 20);
        y2 = Math.max(10 + 50, 20 + 60);
        assertEquals(new Rectangle(x1, y1, x2 - x1, y2 - y1), layer.getModelBounds());

        layer = new Layer();
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(-20, 10, 30, 50)}));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(-10, 20, 20, 60)}));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(1, 2, 40, 50)}));
        x1 = Math.min(Math.min(-20, -10), 1);
        y1 = Math.min(Math.min(10, 20), 2);
        x2 = Math.max(Math.max(-20 + 30, -10 + 20), 1 + 40);
        y2 = Math.max(Math.max(10 + 50, 20 + 60), 2 + 50);
        assertEquals(new Rectangle(x1, y1, x2 - x1, y2 - y1), layer.getModelBounds());
    }

    @Test
    public void testRenderRecognisesVisibileState() {
        final Layer layer = new Layer();
        final RenderCountingLayer l1 = new RenderCountingLayer();
        final RenderCountingLayer l2 = new RenderCountingLayer();
        final RenderCountingLayer l3 = new RenderCountingLayer();

        layer.getChildren().add(l1);
        layer.getChildren().add(l2);
        layer.getChildren().add(l3);

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

        final java.util.List<Layer> list = new Layer().getChildren();

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
        final List<Layer> list = owner.getChildren();


        final TracingLayerListener ll = new TracingLayerListener();
        owner.addListener(ll);

        list.add(new Layer());
        assertEquals("added 1;", ll.trace);

        list.add(new Layer());
        assertEquals("added 1;added 1;", ll.trace);

        assertSame(owner, list.get(0).getParent());
        assertSame(owner, list.get(1).getParent());

        Layer layer0 = list.remove(0);
        assertNull(layer0.getParent());
        assertEquals("added 1;added 1;removed 1;", ll.trace);

        layer0 = list.set(0, new Layer());
        assertNull(layer0.getParent());
        assertEquals("added 1;added 1;removed 1;removed 1;added 1;", ll.trace);
    }

    public static class RenderCountingLayer extends Layer {
        int renderCount;

        @Override
        protected void renderLayer(Rendering rendering) {
            renderCount++;
        }
    }
}