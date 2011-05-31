package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.geom.Point2D;

/**
 * The graphical representation of point figures.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public interface Symbol {

    /**
     * Draws the symbol on the given rendering using the given style.
     * The rendering's graphics is transformed so that
     * drawing can be done in view coordinates relative to the point
     * this symbol represents..
     *
     * @param rendering The rendering.
     * @param style     The style.
     */
    void draw(Rendering rendering, FigureStyle style);

    /**
     * Tests weather this symbol is hit by the given point. The point is provided
     * in view coordinates relative to the point this symbol represents.
     *
     * @param x in <i>symbol</i> coordinates.
     * @param y in <i>symbol</i> coordinates.
     * @return {@code true}, if so.
     */
    boolean isHitBy(double x, double y);
}
