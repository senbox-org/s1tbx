package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Symbol;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * A symetric point symbol that has its reference point in its center.
 */
public class PointSymbol implements Symbol {

    private final int type;
    private final double r;
    private final double a;

    public static PointSymbol createPlus(double size) {
        return new PointSymbol(size, 0x01);
    }

    public static PointSymbol createCross(double size) {
        return new PointSymbol(size, 0x02);
    }

    public static PointSymbol createStar(double size) {
        return new PointSymbol(size, 0x01 | 0x02);
    }

    private PointSymbol(double size, int type) {
        this.type = type;
        this.r = 0.5 * size;
        this.a = r / Math.sqrt(2.0);
    }

    @Override
    public void draw(Rendering rendering, FigureStyle style) {
        rendering.getGraphics().setStroke(style.getStroke());
        rendering.getGraphics().setPaint(style.getStrokePaint());
        if ((type & 0x01) != 0) {
            rendering.getGraphics().draw(new Line2D.Double(-r, 0.0, +r, 0.0));
            rendering.getGraphics().draw(new Line2D.Double(0.0, -r, 0.0, +r));
        }
        if ((type & 0x02) != 0) {
            rendering.getGraphics().draw(new Line2D.Double(-a, -a, +a, +a));
            rendering.getGraphics().draw(new Line2D.Double(+a, -a, -a, +a));
        }
    }

    @Override
    public boolean isHitBy(double x, double y) {
        return x * x + y * y < r * r;
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(-r, -r, 2.0 * r, 2.0 * r);
    }
}
