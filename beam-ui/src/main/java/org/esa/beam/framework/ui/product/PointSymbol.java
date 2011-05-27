package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.geom.Point2D;

/**
 * The graphical representation of point figures.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public interface PointSymbol {

    /**
     * @return The reference X coordinate.
     */
    double getRefX();

    /**
     * @return The reference Y coordinate.
     */
    double getRefY();

    /**
     * Draws the symbol on the given rendering using the given style.
     *
     * @param rendering The rendering.
     * @param style     The style.
     */
    void draw(Rendering rendering, FigureStyle style);

    /**
     * Tests weather the given point is contained in this symbol.
     *
     * @param x in <i>symbol</i> coordinates.
     * @param y in <i>symbol</i> coordinates.
     * @return {@code true}, if so.
     */
    boolean containsPoint(double x, double y);
}
