/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.support.BufferedImageRendering;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CollectionLayerTest {

    @Test
    public void testCollection() {
        Layer collectionLayer = new CollectionLayer();
        assertTrue(collectionLayer.isCollectionLayer());
    }

    @Test
    public void testModelBounds() {
        Layer layer;
        int x1;
        int x2;
        int y1;
        int y2;

        layer = new CollectionLayer();
        assertEquals(null, layer.getModelBounds());

        layer = new CollectionLayer();
        layer.getChildren().add(new CollectionLayer());
        layer.getChildren().add(new CollectionLayer());
        layer.getChildren().add(new CollectionLayer());
        assertEquals(null, layer.getModelBounds());

        layer = new CollectionLayer();
        layer.getChildren().add(new CollectionLayer());
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(20, 10, 30, 50)}, new AffineTransform()));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(10, 20, 20, 60)}, new AffineTransform()));
        x1 = Math.min(20, 10);
        y1 = Math.min(10, 20);
        x2 = Math.max(20 + 30, 10 + 20);
        y2 = Math.max(10 + 50, 20 + 60);
        assertEquals(new Rectangle(x1, y1, x2 - x1, y2 - y1), layer.getModelBounds());

        layer = new CollectionLayer();
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(-20, 10, 30, 50)}, new AffineTransform()));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(-10, 20, 20, 60)}, new AffineTransform()));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(1, 2, 40, 50)}, new AffineTransform()));
        x1 = Math.min(Math.min(-20, -10), 1);
        y1 = Math.min(Math.min(10, 20), 2);
        x2 = Math.max(Math.max(-20 + 30, -10 + 20), 1 + 40);
        y2 = Math.max(Math.max(10 + 50, 20 + 60), 2 + 50);
        assertEquals(new Rectangle(x1, y1, x2 - x1, y2 - y1), layer.getModelBounds());
    }

    @Test
    public void testRenderRecognisesVisibileState() {
        final Layer layer = new CollectionLayer();
        final RenderCountingLayer l1 = new RenderCountingLayer();
        final RenderCountingLayer l2 = new RenderCountingLayer();
        final RenderCountingLayer l3 = new RenderCountingLayer();

        layer.getChildren().add(l1);
        layer.getChildren().add(l2);
        layer.getChildren().add(l3);

        final Rendering rendering = new BufferedImageRendering(16, 16);
        layer.render(rendering);
        assertEquals(1, l1.getRenderCount());
        assertEquals(1, l2.getRenderCount());
        assertEquals(1, l3.getRenderCount());

        l2.setVisible(false);
        layer.render(rendering);
        assertEquals(2, l1.getRenderCount());
        assertEquals(1, l2.getRenderCount());
        assertEquals(2, l3.getRenderCount());

        l3.setVisible(false);
        layer.render(rendering);
        assertEquals(3, l1.getRenderCount());
        assertEquals(1, l2.getRenderCount());
        assertEquals(2, l3.getRenderCount());

        layer.setVisible(false);
        layer.render(rendering);
        assertEquals(3, l1.getRenderCount());
        assertEquals(1, l2.getRenderCount());
        assertEquals(2, l3.getRenderCount());
    }

    @Test
    public void testListInterfaceImplementionSpecConformance() {
        final Layer layer = new CollectionLayer();
        final java.util.List<Layer> list = layer.getChildren();

        assertEquals(true, list.isEmpty());
        assertEquals(0, list.size());

        Iterator<Layer> layerIterator = list.iterator();
        assertNotNull(layerIterator);
        assertEquals(false, layerIterator.hasNext());
        try {
            layerIterator.next();
            fail();
        } catch (NoSuchElementException ignored) {
            // ok
        }

        Layer childLayer0 = new CollectionLayer();
        list.add(childLayer0);

        assertEquals(false, list.isEmpty());
        assertEquals(1, list.size());

        layerIterator = list.iterator();
        assertNotNull(layerIterator);
        assertEquals(true, layerIterator.hasNext());
        assertSame(childLayer0, layerIterator.next());
        assertEquals(false, layerIterator.hasNext());
        try {
            layerIterator.next();
            fail();
        } catch (NoSuchElementException ignored) {
            // ok
        }

        assertSame(childLayer0, list.get(0));
        try {
            list.get(1);
            fail();
        } catch (IndexOutOfBoundsException ignored) {
            // ok
        }

        Layer childLayer1 = new CollectionLayer();
        list.add(childLayer1);

        assertEquals(2, list.size());
        assertSame(childLayer0, list.get(0));
        assertSame(childLayer1, list.get(1));

        childLayer0 = new CollectionLayer();
        list.set(0, childLayer0);
        assertSame(childLayer0, list.get(0));
        assertSame(childLayer1, list.get(1));

        list.remove(childLayer0);
        assertEquals(1, list.size());
        assertSame(childLayer1, list.get(0));

        list.remove(0);
        assertEquals(0, list.size());

        list.add(new CollectionLayer());
        list.add(new CollectionLayer());
        list.add(new CollectionLayer());
        final Iterator<Layer> iterator = list.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertEquals(0, list.size());
    }

    @Test
    public void testChildLayerListIsLife() {

        Layer owner = new CollectionLayer();
        final List<Layer> list = owner.getChildren();


        final TracingLayerListener ll = new TracingLayerListener();
        owner.addListener(ll);

        list.add(new CollectionLayer());
        assertEquals("added 1;", ll.trace);

        list.add(new CollectionLayer());
        assertEquals("added 1;added 1;", ll.trace);

        assertSame(owner, list.get(0).getParent());
        assertSame(owner, list.get(1).getParent());

        Layer layer0 = list.remove(0);
        assertNull(layer0.getParent());
        assertEquals("added 1;added 1;removed 1;", ll.trace);

        layer0 = list.set(0, new CollectionLayer());
        assertNull(layer0.getParent());
        assertEquals("added 1;added 1;removed 1;removed 1;added 1;", ll.trace);
    }

    private static class RenderCountingLayer extends CollectionLayer {

        private int renderCount;

        @Override
        protected void renderLayer(Rendering rendering) {
            renderCount++;
        }

        public int getRenderCount() {
            return renderCount;
        }
    }
}
