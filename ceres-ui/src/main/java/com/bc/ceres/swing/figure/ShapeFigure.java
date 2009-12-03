package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.undo.Restorable;
import com.bc.ceres.grender.Rendering;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


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