package com.bc.ceres.glayer;

import static junit.framework.Assert.*;

import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.awt.image.BufferedImage;

import com.bc.ceres.glayer.support.ImageLayer;
import org.junit.Test;

public class LayerChildrenTest {
    @Test
    public void testListInterfaceImplementionSpecConformance() {

        final List<Layer> list = createLayer().getChildLayers();

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

        Layer layer0 = createLayer();
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

        Layer layer1 = createLayer();
        list.add(layer1);

        assertEquals(2, list.size());
        assertSame(layer0, list.get(0));
        assertSame(layer1, list.get(1));

        layer0 = createLayer();
        list.set(0, layer0);
        assertSame(layer0, list.get(0));
        assertSame(layer1, list.get(1));

        list.remove(layer0);
        assertEquals(1, list.size());
        assertSame(layer1, list.get(0));

        list.remove(0);
        assertEquals(0, list.size());

        list.add(createLayer());
        list.add(createLayer());
        list.add(createLayer());
        final Iterator<Layer> iterator = list.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertEquals(0, list.size());
        

    }

    @Test
    public void testListIsLife() {

        Layer owner = createLayer();
        final List<Layer> list = owner.getChildLayers();


        final TracingLayerListener ll = new TracingLayerListener();
        owner.addListener(ll);

        list.add(createLayer());
        assertEquals("added 1;", ll.trace);

        list.add(createLayer());
        assertEquals("added 1;added 1;", ll.trace);

        assertSame(owner, list.get(0).getParentLayer());
        assertSame(owner, list.get(1).getParentLayer());

        Layer layer0 = list.remove(0);
        assertNull(layer0.getParentLayer());
        assertEquals("added 1;added 1;removed 1;", ll.trace);

        layer0 = list.set(0, createLayer());
        assertNull(layer0.getParentLayer());
        assertEquals("added 1;added 1;removed 1;removed 1;added 1;", ll.trace);
    }

    private static Layer createLayer() {
        return new ImageLayer(new BufferedImage(16,16, BufferedImage.TYPE_3BYTE_BGR));
    }

}