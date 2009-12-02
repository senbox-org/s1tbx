package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.undo.Restorable;
import com.bc.ceres.grender.Rendering;

import java.awt.Graphics2D;
import java.awt.Shape;
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

    // Temporarily solution
    // todo - (1) remove or
    // todo - (2) rename to getGeometryAsShape()
    Shape getShape();

    // Temporarily solution
    // todo - (1) remove or
    // todo - (2) rename to setGeometryFromShape()
    void setShape(Shape shape);

    boolean isSelectable();

    boolean isSelected();

    void setSelected(boolean selected);

    void draw(Rendering rendering);

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

    Figure[] getFigures(Shape shape);

    boolean addFigure(Figure figure);

    boolean addFigure(int index, Figure figure);

    Figure[] addFigures(Figure... figures);

    boolean removeFigure(Figure figure);

    Figure[] removeFigures(Figure... figures);

    Figure[] removeFigures();

    int getMaxSelectionStage();

    Handle[] createHandles(int selectionStage);

    void addListener(FigureChangeListener l);

    void removeListener(FigureChangeListener listener);

    FigureChangeListener[] getListeners();

    void dispose();

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Object clone();
}
