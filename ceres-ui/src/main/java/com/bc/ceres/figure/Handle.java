package com.bc.ceres.figure;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

// todo - extract common interface for Figure/Handle
/**
 * A handle is a graphical modifier for {@link Figure}s.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Handle {
    boolean isSelectable();

    boolean isSelected();

    void setSelected(boolean selected);

    void draw(Graphics2D g2d);

    boolean contains(Point2D point);

    void move(double dx, double dy);

    Cursor getCursor();

    void dispose();
}
