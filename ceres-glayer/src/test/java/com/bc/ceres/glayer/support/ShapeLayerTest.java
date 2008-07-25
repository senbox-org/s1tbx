package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.ImagingTestCase;
import com.bc.ceres.glayer.support.ShapeLayer;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class ShapeLayerTest extends ImagingTestCase {
    public void testConstructors() {
        ShapeLayer layer;

        final Shape shape = new Rectangle2D.Double(0, 0, 320.0, 200.0);

        layer = new ShapeLayer(new Shape[]{shape});
        final List<Shape> shapeList = layer.getShapeList();
        assertNotNull(shapeList);
        assertNotSame(shapeList, layer.getShapeList());
        assertEquals(1, shapeList.size());
        assertSame(shape, shapeList.get(0));

        final AffineTransform s2u = AffineTransform.getTranslateInstance(+100, +200);
        layer = new ShapeLayer(new Shape[]{shape}, s2u);
        assertEquals(AffineTransform.getTranslateInstance(+100, +200), layer.getShapeToModelTransform());
        assertEquals(AffineTransform.getTranslateInstance(-100, -200), layer.getModelToShapeTransform());
    }

    public void testBoundingBox() {
        ShapeLayer layer;

        final Shape shape = new Rectangle2D.Double(0.0, 0.0, 320.0, 200.0);

        layer = new ShapeLayer(new Shape[]{shape});
        assertNotNull(layer.getBounds());
        assertEquals(new Rectangle2D.Double(0.0, 0.0, 320.0, 200.0), layer.getBounds());

        final AffineTransform s2u = new AffineTransform(0.5, 0, 0, 0.5, -25.5, 50.3);
        layer = new ShapeLayer(new Shape[]{shape}, s2u);
        assertNotNull(layer.getBounds());
        assertEquals(new Rectangle2D.Double(-25.5, 50.3, 160.0, 100.0), layer.getBounds());
    }
}