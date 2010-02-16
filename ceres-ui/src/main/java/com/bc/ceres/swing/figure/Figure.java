package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.undo.Restorable;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
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

    ///////////////////////////////////////////
    // {{
    // todo - interface FigureGeometry?

    enum Rank {

        NOT_SPECIFIED(-1),
        POINT(0),
        LINE(1),
        AREA(2);

        public final int value;

        private Rank(int value) {
            this.value = value;
        }
    }

    boolean isCollection();

    boolean contains(Figure figure);

    /**
     * Tests if the given point is "close to" the given point.
     * @param point Point in model coordinates.
     * @param m2v  Current model-to-view transformation.
     * @return The figure, or {@code null}.
     */
    boolean isCloseTo(Point2D point, AffineTransform m2v);

    Rectangle2D getBounds();

    Rank getRank();

    void move(double dx, double dy);

    void scale(Point2D point, double sx, double sy);

    void rotate(Point2D point, double theta);

    double[] getSegment(int index);

    void setSegment(int index, double[] segment);

    void addSegment(int index, double[] segment);

    void removeSegment(int index);
    // }}
    ///////////////////////////////////////////

    boolean isSelectable();

    boolean isSelected();

    void setSelected(boolean selected);

    void draw(Rendering rendering);

    int getFigureCount();

    int getFigureIndex(Figure figure);

    Figure getFigure(int index);

    /**
     * Gets the "nearest" figure for the given point.
     * @param point Point in model coordinates.
     * @param m2v  Current model-to-view transformation.
     * @return The figure, or {@code null}.
     */
    Figure getFigure(Point2D point, AffineTransform m2v);

    Figure[] getFigures();

    Figure[] getFigures(Shape shape);

    boolean addFigure(Figure figure);

    boolean addFigure(int index, Figure figure);

    Figure[] addFigures(Figure... figures);

    boolean removeFigure(Figure figure);

    Figure[] removeFigures(Figure... figures);

    Figure[] removeAllFigures();

    int getMaxSelectionStage();

    Handle[] createHandles(int selectionStage);

    void addChangeListener(FigureChangeListener listener);

    void removeChangeListener(FigureChangeListener listener);

    FigureChangeListener[] getChangeListeners();

    void dispose();

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Object clone();
}
