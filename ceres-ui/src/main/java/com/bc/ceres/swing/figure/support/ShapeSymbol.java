package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Symbol;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * A point symbol that is represented by a Java AWT shape geometry.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public class ShapeSymbol implements Symbol {
    private final Shape shape;

    public static ShapeSymbol createCircle(double size) {
        return new ShapeSymbol(new Ellipse2D.Double(-0.5 * size, -0.5 * size, size, size));
    }

    public static ShapeSymbol createSquare(double size) {
        return new ShapeSymbol(new Rectangle2D.Double(-0.5 * size, -0.5 * size, size, size));
    }

    public static ShapeSymbol createPin(double size) {
        final double knobSize = size / 2.0;
        final double h34 = (3.0 / 4.0) * size;
        final double h14 = (1.0 / 4.0) * size;
        final GeneralPath path = new GeneralPath();
        path.moveTo(0.0, size);
        path.lineTo(h34 - 1.0, h14 - 1.0);
        path.lineTo(h34 + 1.0, h14 + 1.0);
        path.closePath();
        final Ellipse2D.Double knob = new Ellipse2D.Double(h34 - 0.5 * knobSize, h14 - 0.5 * knobSize, knobSize, knobSize);
        final Area needle = new Area(path);
        needle.subtract(new Area(knob));
        final GeneralPath pin = new GeneralPath();
        pin.append(needle, false);
        pin.append(knob, false);
        Shape shape = AffineTransform.getTranslateInstance(0.0, -size).createTransformedShape(pin);
        return new ShapeSymbol(shape);
    }

    public ShapeSymbol(Shape shape) {
        this.shape = shape;
    }

    public Shape getShape() {
        return shape;
    }

    @Override
    public void draw(Rendering rendering, FigureStyle style) {
        if (style.getFillOpacity() > 0.0) {
            rendering.getGraphics().setPaint(style.getFillPaint());
            rendering.getGraphics().fill(shape);
        }
        if (style.getStrokeOpacity() > 0.0) {
            rendering.getGraphics().setStroke(style.getStroke());
            rendering.getGraphics().setPaint(style.getStrokePaint());
            rendering.getGraphics().draw(shape);
        }
    }

    @Override
    public boolean isHitBy(double x, double y) {
        return shape.contains(x, y);
    }

    @Override
    public Rectangle2D getBounds() {
        return shape.getBounds2D();
    }
}
