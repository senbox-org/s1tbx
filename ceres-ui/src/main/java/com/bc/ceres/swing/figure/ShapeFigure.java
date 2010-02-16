package com.bc.ceres.swing.figure;

import java.awt.Shape;


/**
 * A figure represents a graphical object.
 * Figures are graphically modified by their {@link com.bc.ceres.swing.figure.Handle}s.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface ShapeFigure extends Figure {

    Shape getShape();

    void setShape(Shape shape);
}