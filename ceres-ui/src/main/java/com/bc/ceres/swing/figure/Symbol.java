package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The graphical representation of point figures.
 * Symbol geometry is provided in symbol coordinates using view units.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public interface Symbol {

    /**
     * Draws the symbol on the given rendering using the given style.
     * The rendering's graphics is transformed so that
     * drawing can be done directly in symbol coordinates using view units, e.g.
     * <pre>
     *     rendering.getGraphics().draw(symbolShape);
     * </pre>
     * or
     * <pre>
     *     rendering.getGraphics().drawRenderedImage(symbolImage, null);
     * </pre>
     *
     * @param rendering The rendering.
     * @param style     The style.
     */
    void draw(Rendering rendering, FigureStyle style);

    /**
     * Tests weather this symbol is hit by the given point.
     *
     * @param x The X-coordinate of the point in symbol coordinates using view units.
     * @param y The Y-coordinate of the point in symbol coordinates using view units.
     * @return {@code true}, if so.
     */
    boolean isHitBy(double x, double y);

    /**
     * The bounds of the symbol.
     *
     * @return The bounds of the symbol in symbol coordinates using view units.
     */
    Rectangle2D getBounds();
}
