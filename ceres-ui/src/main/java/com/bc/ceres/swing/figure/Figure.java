/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
 * <p>
 * Clients should not implement this interface directly, because it may change in the future.
 * Instead they should derive their {@code Figure} implementation from {@link AbstractFigure}.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Figure extends Restorable, Cloneable {

    /**
     * The rang of a figure.
     */
    enum Rank {

        /**
         * Figure rank has not explicitly been specified.
         */
        NOT_SPECIFIED(-1),
        /**
         * Figure rank is 0-dimensional point.
         */
        POINT(0),
        /**
         * Figure rank is a 1-dimensional line.
         */
        LINE(1),
        /**
         * Figure rank is a 2-dimensional area.
         */
        AREA(2);

        public final int value;

        private Rank(int value) {
            this.value = value;
        }
    }

    /**
     * @return The style used for the "normal" state of the figure.
     * @since Ceres 0.13
     */
    FigureStyle getNormalStyle();

    /**
     * Sets the style used for the "normal" state of the figure.
     * @param normalStyle The style used for the "normal" state of the figure.
     * @since Ceres 0.13
     */
    void setNormalStyle(FigureStyle normalStyle);

    /**
     * @return The style used for the "selected" state of the figure.
     * @since Ceres 0.13
     */
    FigureStyle getSelectedStyle();

    /**
     * Sets the style used for the "selected" state of the figure.
     * @param selectedStyle The style used for the "selected" state of the figure.
     * @since Ceres 0.13
     */
    void setSelectedStyle(FigureStyle selectedStyle);

    /**
     * @return The effective style used for the current state of the figure.
     * @since Ceres 0.13
     */
    FigureStyle getEffectiveStyle();

    /**
     * Tests if this figure is a figure collection.
     *
     * @return {@code true}, if so.
     */
    boolean isCollection();

    /**
     * Tests if this figure fully contains the given figure.
     *
     * @param figure A figure.
     * @return {@code true}, if the given figure is fully contained in this figure.
     */
    boolean contains(Figure figure);

    /**
     * Tests if the given point is "close to" this figure.
     *
     * @param point Point in model coordinates.
     * @param m2v   Current model-to-view transformation.
     * @return {@code true}, if the point is close to this figure.
     */
    boolean isCloseTo(Point2D point, AffineTransform m2v);

    /**
     * @return The figure bounds in model coordinates.
     */
    Rectangle2D getBounds();

    /**
     * @return The figure's rank.
     */
    Rank getRank();

    /**
     * Moves the figure by the given delta in model coordinates.
     *
     * @param dx Delta X in model coordinates.
     * @param dy Delta Y in model coordinates.
     */
    void move(double dx, double dy);

    /**
     * Scales the figure by the given scale factors.
     *
     * @param point The reference point in model coordinates.
     * @param sx    Scale X factor.
     * @param sy    Scale Y factor.
     */
    void scale(Point2D point, double sx, double sy);

    /**
     * Rotates the figure by the given angle.
     *
     * @param point The reference point in model coordinates.
     * @param theta The rotation angle in degree.
     */
    void rotate(Point2D point, double theta);

    /**
     * Gets the segment at the given vertex index.
     *
     * @param index The vertex index.
     * @return The segment coordinates. X is the first element, Y the second.
     */
    double[] getSegment(int index);

    /**
     * Sets the segment at the given vertex index.
     *
     * @param index   The vertex index.
     * @param segment The segment coordinates. X is the first element, Y the second.
     */
    void setSegment(int index, double[] segment);

    /**
     * Adds the segment at the given vertex index.
     *
     * @param index   The vertex index.
     * @param segment The segment coordinates. X is the first element, Y the second.
     */
    void addSegment(int index, double[] segment);

    /**
     * Removes the segment at the given vertex index.
     *
     * @param index The vertex index.
     */
    void removeSegment(int index);

    /**
     * Tests if the figure is selectable.
     *
     * @return {@code true}, if so.
     */
    boolean isSelectable();

    /**
     * Tests if the figure is selected.
     *
     * @return {@code true}, if so.
     */
    boolean isSelected();

    /**
     * Sets the selected state.
     *
     * @param selected The selected state.
     */
    void setSelected(boolean selected);

    /**
     * Draws this figure using the given rendering.
     *
     * @param rendering The rendering used to draw the figure.
     */
    void draw(Rendering rendering);

    /**
     * @return The number of child figures this figure has.
     */
    int getFigureCount();

    /**
     * Gets the index of the given child figure.
     *
     * @param figure The child figure to look up.
     * @return The index, {@code -1}, if the figure ios not a child.
     */
    int getFigureIndex(Figure figure);

    /**
     * Gets the child figure at the given index.
     *
     * @param index The child index.
     * @return The child figure.
     */
    Figure getFigure(int index);

    /**
     * Gets the "nearest" figure for the given point.
     *
     * @param point Point in model coordinates.
     * @param m2v   Current model-to-view transformation.
     * @return The figure, or {@code null}.
     */
    Figure getFigure(Point2D point, AffineTransform m2v);

    /**
     * @return The array of child figures. An empty array, if this figure does not have child figures.
     */
    Figure[] getFigures();

    /**
     * Gets child figures that have an intersection with the given shape.
     *
     * @param shape The shape in model coordinates.
     * @return The array of child figures that have an intersection with the given shape.
     */
    Figure[] getFigures(Shape shape);

    /**
     * Adds a child figure to this figure.
     *
     * @param figure The new child figure.
     * @return {@code true}, if the child has been added.
     */
    boolean addFigure(Figure figure);

    /**
     * Adds a child figure at the given index to this figure.
     *
     * @param index  The index.
     * @param figure The new child figure.
     * @return {@code true}, if the child has been added.
     */
    boolean addFigure(int index, Figure figure);

    /**
     * Adds a child figure at the given index to this figure.
     *
     * @param figures The array of new child figures.
     * @return The array of child figures that actually have been added.
     */
    Figure[] addFigures(Figure... figures);

    /**
     * Removes a child figure from this figure.
     *
     * @param figure The new child figure.
     * @return {@code true}, if the child has been removed.
     */
    boolean removeFigure(Figure figure);

    /**
     * Removes the given child figures from this figure.
     *
     * @param figures The array of child figures to remove.
     * @return The array of child figures that actually have been removed.
     */
    Figure[] removeFigures(Figure... figures);

    /**
     * Removes all child figures from this figure.
     *
     * @return The array of child figures that actually have been removed.
     */
    Figure[] removeAllFigures();

    /**
     * Gets the maximum number of selection stages offered by this figure. A figure may enter into a new selection stage
     * if it is already selected and is then selected again (e.g. by clicking it once more).
     *
     * @return The maximum number of selection stages.
     */
    int getMaxSelectionStage();

    /**
     * Creates the handles for a given selection stage.
     *
     * @param selectionStage The selection stage.
     * @return The array of handles.
     */
    Handle[] createHandles(int selectionStage);

    /**
     * Adds a new change listener to this figure.
     *
     * @param listener The listener.
     */
    void addChangeListener(FigureChangeListener listener);

    /**
     * Removes a change listener from this figure.
     *
     * @param listener The listener.
     */
    void removeChangeListener(FigureChangeListener listener);

    /**
     * @return The array of all change listeners registered with this figure.
     */
    FigureChangeListener[] getChangeListeners();

    /**
     * Disposes this figure. Indicates that it will no longer be used.
     */
    void dispose();

    /**
     * @return A clone of this figure.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Object clone();

}
