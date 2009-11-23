package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Only implements abstract Figure methods, no overrides!
 */
class AbstractFigureImpl extends AbstractFigure {

    public AbstractFigureImpl() {
    }


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
        return Rank.PUNCTUAL;
    }

    void postChangeEvent() {
        fireFigureChanged();
    }
}