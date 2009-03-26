/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.glayer;

import static org.junit.Assert.*;
import static com.bc.ceres.glayer.Assert2D.*;

import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.support.BufferedImageRendering;

import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CollectionLayerTest {
    
    private static LayerType collectionLayerType;
    
    @BeforeClass
    public static void setUpClass() {
        collectionLayerType = LayerType.getLayerType(CollectionLayer.Type.class.getName());
    }
    
    @Test
    public void testCollection() {
        Layer collectionLayer = new CollectionLayer(collectionLayerType, "test");
        assertTrue(collectionLayer.isCollectionLayer());
        
        Layer normalLayer = new Layer();
        assertFalse(normalLayer.isCollectionLayer());
    }

    @Test
    public void testModelBounds() {
        Layer layer;
        int x1, x2, y1, y2;

        layer = new Layer();
        assertEquals(null, layer.getModelBounds());

        layer = new CollectionLayer(collectionLayerType, "test");
        layer.getChildren().add(new Layer());
        layer.getChildren().add(new Layer());
        layer.getChildren().add(new Layer());
        assertEquals(null, layer.getModelBounds());

        layer = new CollectionLayer(collectionLayerType, "test");
        layer.getChildren().add(new Layer());
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(20, 10, 30, 50)}));
        layer.getChildren().add(new ShapeLayer(new Shape[]{new Rectangle(10, 20, 20, 60)}));
        x1 = Math.min(20, 10);
        y1 = Math.min(10, 20);
        x2 = Math.max(20 + 30, 10 + 20);
        y2 = Math.max(10 + 50, 20 + 60);
        assertEquals(new Rectangle(x1, y1, x2 - x1, y2 - y1), layer.getModelBounds());

        layer = new CollectionLayer(collectionLayerType, "test");
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
        final Layer layer = new CollectionLayer(collectionLayerType, "test");
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
    public void testListInterfaceImplementionSpecConformance() {
        final Layer layer = new CollectionLayer(collectionLayerType, "test");
        final java.util.List<Layer> list = layer.getChildren();

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

        Layer childLayer0 = new Layer();
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
        } catch (NoSuchElementException e) {
            // ok
        }

        assertSame(childLayer0, list.get(0));
        try {
            list.get(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        Layer childLayer1 = new Layer();
        list.add(childLayer1);

        assertEquals(2, list.size());
        assertSame(childLayer0, list.get(0));
        assertSame(childLayer1, list.get(1));

        childLayer0 = new Layer();
        list.set(0, childLayer0);
        assertSame(childLayer0, list.get(0));
        assertSame(childLayer1, list.get(1));

        list.remove(childLayer0);
        assertEquals(1, list.size());
        assertSame(childLayer1, list.get(0));

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

        Layer owner = new CollectionLayer(collectionLayerType, "test");
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

    private static class RenderCountingLayer extends Layer {
        int renderCount;

        @Override
        protected void renderLayer(Rendering rendering) {
            renderCount++;
        }
    }
}
