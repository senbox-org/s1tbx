package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TestFigure extends AbstractFigure {
    Shape shape = new Ellipse2D.Double(0, 0, 10, 10);
    boolean selectable;
    boolean selected;

    public TestFigure() {
        this(true);
    }

    public TestFigure(boolean selectable) {
        this.selectable = selectable;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
        fireFigureChanged();
    }

    @Override
    public boolean isSelectable() {
        return selectable;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void draw(Rendering rendering) {
    }

    @Override
    public boolean contains(Point2D point) {
        return shape.contains(point);
    }

    @Override
    public Rectangle2D getBounds() {
        return shape.getBounds2D();
    }

    @Override
    public Rank getRank() {
        return Rank.POLYGONAL;
    }

}
