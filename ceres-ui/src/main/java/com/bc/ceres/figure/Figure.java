package com.bc.ceres.figure;

import com.bc.ceres.undo.Restorable;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * A figure represents a graphical object.
 * Figures are graphically modified by their {@link Handle}s.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Figure extends Restorable, Cloneable {
    enum Rank {
        PUNCTUAL,
        LINEAL,
        POLYGONAL,
        COLLECTION
    }

    boolean isSelected();

    void setSelected(boolean selected);

    void draw(Graphics2D g2d);

    boolean contains(Figure figure);

    boolean contains(Point2D point);

    Rectangle2D getBounds();

    Rank getRank();

    void move(double dx, double dy);

    void scale(Point2D point, double sx, double sy);

    void rotate(Point2D point, double theta);

    // todo - why not use Point2D
    double[] getVertex(int index);

    // todo - why not use Point2D
    void setVertex(int index, double[] vertex);

    int getFigureCount();

    int getFigureIndex(Figure figure);

    Figure getFigure(int index);

    Figure getFigure(Point2D p);

    Figure[] getFigures();

    Figure[] getFigures(Rectangle2D rectangle);

    boolean addFigure(Figure figure);

    boolean addFigure(int index, Figure figure);

    Figure[] addFigures(Figure... figures);

    boolean removeFigure(Figure figure);

    Figure[] removeFigures(Figure... figures);

    Figure[] removeFigures();

    int getMaxSelectionLevel();

    Handle[] createHandles(int selectionLevel);

    void addListener(FigureChangeListener l);

    void removeListener(FigureChangeListener listener);

    FigureChangeListener[] getListeners();

    void dispose();

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Object clone();
}
