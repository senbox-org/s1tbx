package com.bc.ceres.glayer.support;

import static org.junit.Assert.*;
import org.junit.Test;
import static com.bc.ceres.glayer.Assert2D.*;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class ShapeLayerTest  {
    @Test
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

    @Test
    public void testModelBounds() {
        ShapeLayer layer;


        layer = new ShapeLayer(new Shape[0]);
        assertEquals(null, layer.getModelBounds());

        Shape shape = new Rectangle(10, -30, 320, 200);
        layer = new ShapeLayer(new Shape[]{shape});
        assertNotNull(layer.getModelBounds());
        assertEquals(new Rectangle(10, -30, 320, 200), layer.getModelBounds());

        final AffineTransform s2m = new AffineTransform(0.5, 0, 0, 0.5, -25.5, 50.3);
        layer = new ShapeLayer(new Shape[]{shape}, s2m);
        assertNotNull(layer.getModelBounds());
        assertEquals(new Rectangle2D.Double(10 * 0.5 -25.5, -30 * 0.5 + 50.3, 320 * 0.5, 200*0.5), layer.getModelBounds());
    }
}