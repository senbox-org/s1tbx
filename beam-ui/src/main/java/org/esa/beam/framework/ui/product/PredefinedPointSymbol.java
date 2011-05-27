package org.esa.beam.framework.ui.product;

/**
 * Created by IntelliJ IDEA.
 * User: nfomferra
 * Date: 5/27/11
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */
public enum PredefinedPointSymbol {

    PLUS(1, CrossSymbol.createPlus(8.0)),
    CROSS(2, CrossSymbol.createCross(8.0)),
    STAR(3, CrossSymbol.createStar(8.0)),
    SQUARE(4, ShapeSymbol.createSquare(8.0)),
    CIRCLE(5, ShapeSymbol.createCircle(8.0));

    public int getIndex() {
        return index;
    }

    public PointSymbol getPointSymbol() {
        return pointSymbol;
    }

    private final int index;
    private final PointSymbol pointSymbol;

    private PredefinedPointSymbol(int index, PointSymbol pointSymbol) {
        this.index = index;
        this.pointSymbol = pointSymbol;
    }
}
