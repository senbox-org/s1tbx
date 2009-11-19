package com.bc.ceres.figure;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;


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
