package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * A handle is a graphical modifier for {@link Figure}s.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Handle extends ShapeFigure {
    Point2D getLocation();

    Cursor getCursor();
}
