package com.bc.ceres.glayer;

import com.bc.ceres.glayer.ImagingTestCase;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.ShapeLayer;
import com.bc.ceres.glayer.Viewport;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;


public class CollectionLayerTest extends ImagingTestCase {

    public void testConstructor() {
        final CollectionLayer cl = new CollectionLayer();
        assertEquals(true, cl.isEmpty());
        assertEquals(0, cl.size());
        assertNotNull(cl.getBoundingBox());
        assertTrue(cl.getBoundingBox().isEmpty());
    }

    public void testBoundingBox() {
        final CollectionLayer cl = new CollectionLayer();
        cl.add(new ShapeLayer(new Shape[]{new Rectangle(-20, 10, 30, 50)}));
        cl.add(new ShapeLayer(new Shape[]{new Rectangle(-10, 20, 20, 60)}));
        cl.add(new ShapeLayer(new Shape[]{new Rectangle(0, 0, 40, 50)}));
        assertNotNull(cl.getBoundingBox());
        assertEquals(new Rectangle(-20, 0, 60, 80), cl.getBoundingBox());
    }

    public void testAllLayersRendered() {
        final CollectionLayer cl = new CollectionLayer();
        final TestLayer l1 = new TestLayer();
        final TestLayer l2 = new TestLayer();
        final TestLayer l3 = new TestLayer();
        cl.add(l1);
        cl.add(l2);
        cl.add(l3);
        assertEquals(3, cl.size());
        final Rendering rendering = new BufferedImageRendering(16, 16);
        cl.render(rendering);
        assertEquals(1, l1.paintCount);
        assertEquals(1, l2.paintCount);
        assertEquals(1, l3.paintCount);

        l2.setVisible(false);
        cl.render(rendering);
        assertEquals(2, l1.paintCount);
        assertEquals(1, l2.paintCount);
        assertEquals(2, l3.paintCount);

        l3.setVisible(false);
        cl.render(rendering);
        assertEquals(3, l1.paintCount);
        assertEquals(1, l2.paintCount);
        assertEquals(2, l3.paintCount);

        cl.setVisible(false);
        cl.render(rendering);
        assertEquals(3, l1.paintCount);
        assertEquals(1, l2.paintCount);
        assertEquals(2, l3.paintCount);
    }
}
