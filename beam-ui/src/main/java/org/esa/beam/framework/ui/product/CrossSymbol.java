package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.geom.Line2D;

/**
* Created by IntelliJ IDEA.
* User: nfomferra
* Date: 5/27/11
* Time: 4:07 PM
* To change this template use File | Settings | File Templates.
*/
public class CrossSymbol implements PointSymbol {

    private final int type;
    private final double r;
    private final double a;

    public CrossSymbol(double size, int type) {
        this.type = type;
        this.r = size;
        this.a = size / Math.sqrt(2);
    }

    @Override
    public double getRefX() {
        return 0;
    }

    @Override
    public double getRefY() {
        return 0;
    }

    @Override
    public void draw(Rendering rendering, FigureStyle style) {
        rendering.getGraphics().setStroke(style.getStroke());
        if ((type & 0x01) != 0) {
            rendering.getGraphics().draw(new Line2D.Double(-a, -a, +a, +a));
            rendering.getGraphics().draw(new Line2D.Double(+a, -a, -a, +a));
        }
        if ((type & 0x02) != 0) {
            rendering.getGraphics().draw(new Line2D.Double(-r, 0, +r, 0));
            rendering.getGraphics().draw(new Line2D.Double(0, -r, -r, 0));
        }
    }

    @Override
    public boolean containsPoint(double x, double y) {
        return x * x + y * y <= r * r;
    }
}
