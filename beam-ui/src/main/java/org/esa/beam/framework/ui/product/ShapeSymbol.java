package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * A point symbol that is represented by a Java AWT shape geometry.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public class ShapeSymbol implements PointSymbol {
    private final Shape shape;
    private final double refX;
    private final double refY;

    public static ShapeSymbol createCircle(double size) {
        return new ShapeSymbol(new Ellipse2D.Double(-0.5 * size, -0.5 * size, size, size), 0, 0);
    }

    public static ShapeSymbol createSquare(double size) {
        return new ShapeSymbol(new Rectangle2D.Double(-0.5 * size, -0.5 * size, size, size), 0, 0);
    }

    public ShapeSymbol(Shape shape, double refX, double refY) {
        this.shape = shape;
        this.refX = refX;
        this.refY = refY;
    }

    @Override
    public double getRefX() {
        return refX;
    }

    @Override
    public double getRefY() {
        return refY;
    }

    @Override
    public void draw(Rendering rendering, FigureStyle style) {
        rendering.getGraphics().setPaint(style.getFillPaint());
        rendering.getGraphics().fill(shape);
        rendering.getGraphics().setPaint(style.getStrokePaint());
        rendering.getGraphics().draw(shape);
    }

    @Override
    public boolean containsPoint(double x, double y) {
        return shape.contains(x, y);
    }

}
