package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Only implements abstract Figure methods, no overrides!
 */
class AbstractFigureImpl extends AbstractFigure {

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public void setSelected(boolean selected) {
    }

    @Override
    public void draw(Rendering rendering) {
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle();
    }

    @Override
    public Rank getRank() {
        return Rank.PUNTAL;
    }

    void postChangeEvent() {
        fireFigureChanged();
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        return false;
    }
}