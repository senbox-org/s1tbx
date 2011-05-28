package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Symbol;

import java.awt.*;
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
    private final double refX;
    private final double refY;

    public static ShapeSymbol createCircle(double size) {
        return new ShapeSymbol(new Ellipse2D.Double(-0.5 * size, -0.5 * size, size, size), 0, 0);
    }

    public static ShapeSymbol createSquare(double size) {
        return new ShapeSymbol(new Rectangle2D.Double(-0.5 * size, -0.5 * size, size, size), 0, 0);
    }

    public static ShapeSymbol createPin(double needleSize, double knobSize) {
//        pinSymbol.setFillPaint(new Color(128, 128, 255));
//        pinSymbol.setFilled(true);
//        pinSymbol.setOutlineColor(new Color(0, 0, 64));
//        pinSymbol.setOutlineStroke(new BasicStroke(1.0f));
        final double h34 = 3.0 * needleSize / 4.0;
        final double h14 = 1.0 * needleSize / 4.0;
        final GeneralPath path = new GeneralPath();
        path.moveTo(0, needleSize);
        path.lineTo(h34 - 1.0, h14 - 1.0);
        path.lineTo(h34 + 1.0, h14 + 1.0);
        path.closePath();
        final Ellipse2D.Double knob = new Ellipse2D.Double(h34 - 0.5 * knobSize, h14 - 0.5 * knobSize, knobSize, knobSize);
        final Area needle = new Area(path);
        needle.subtract(new Area(knob));
        final GeneralPath shape = new GeneralPath();
        shape.append(needle, false);
        shape.append(knob, false);
        return new ShapeSymbol(shape, 0, needleSize);
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
    public boolean containsPoint(double x, double y) {
        return shape.contains(x, y);
    }

}
