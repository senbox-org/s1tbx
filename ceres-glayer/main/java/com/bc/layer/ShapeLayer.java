package com.bc.layer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A shape layer is used to draw {@link Shape}s.
 *
 * @author Norman Fomferra
 */
public class ShapeLayer extends AbstractGraphicalLayer {

    private final List<Shape> shapeList;
    private final AffineTransform shapeToModelTransform;
    private final AffineTransform modelToShapeTransform;

    public ShapeLayer(Shape[] shapes) {
        this.shapeList = new ArrayList<Shape>(Arrays.asList(shapes));
        this.shapeToModelTransform =
                this.modelToShapeTransform = new AffineTransform();
    }

    public ShapeLayer(Shape[] shapes, AffineTransform shapeToModelTransform) {
        this.shapeList = new ArrayList<Shape>(Arrays.asList(shapes));
        this.shapeToModelTransform = (AffineTransform) shapeToModelTransform.clone();
        try {
            this.modelToShapeTransform = shapeToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("imageToModelTransform", e);
        }
    }

    public List<Shape> getShapeList() {
        return new ArrayList<Shape>(shapeList);
    }

    public void setShapeList(List<Shape> list) {
        shapeList.clear();
        shapeList.addAll(list);
    }

    public AffineTransform getShapeToModelTransform() {
        return (AffineTransform) shapeToModelTransform.clone();
    }

    public AffineTransform getModelToShapeTransform() {
        return (AffineTransform) modelToShapeTransform.clone();
    }

    public Rectangle2D getBoundingBox() {
        Rectangle2D boundingBox = new Rectangle2D.Double();
        for (Shape shape : shapeList) {
            boundingBox.add(shape.getBounds2D());
        }
        return shapeToModelTransform.createTransformedShape(boundingBox).getBounds2D();
    }

    @Override
    protected void paintLayer(Graphics2D g, Viewport vp) {
        final AffineTransform transformSave = g.getTransform();
        try {
            final AffineTransform transform = new AffineTransform();
            transform.concatenate(vp.getModelToViewTransform());
            transform.concatenate(shapeToModelTransform);
            g.setTransform(transform);
            for (Shape shape : shapeList) {
                g.setPaint(Color.WHITE);
                g.fill(shape);
                g.setPaint(Color.BLACK);
                g.draw(shape);
            }
        } finally {
            g.setTransform(transformSave);
        }
    }
}