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
package org.esa.snap.core.draw;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Map;

/**
 * The interface of a graphical figure. A figure knows its center point, its bounding box and can draw itself. A figure
 * can be composed of several sub-figures. Figures also can have an open ended set of attributes. An attribute is
 * identified by a string and has an arbitrary type.
 * <p>To interact and manipulate with a figure it can provide handles (see {@link FigureHandle}).<p> A
 * handle can manipulate a figure's shape or its attributes.
 * <p>A default implementation for the Figure interface are provided by the <code>AbstractFigure</code> class.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see FigureHandle
 * @see AbstractFigure
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public interface Figure extends Drawable, Cloneable, Serializable {

    /**
     * @deprecated since BEAM 4.6, no replacement
     */
    @Deprecated
    public static final String TOOL_INPUT_EVENT_KEY = "toolInputEvent";

    /**
     * The attribute key used to determine whether or not this figure has a filled interior. The value must be an
     * instance of <code>java.lang.Boolean</code>.
     */
    public static final String FILLED_KEY = "filled";
    /**
     * The attribute key used to determine which fill pattern to use for the shape's interior. The value must be an
     * instance of <code>java.awt.Paint</code>.
     */
    public static final String FILL_PAINT_KEY = "fill_paint";
    /**
     * The attribute key used to determine which stroke to use for the shape's interior. The value must be an instance
     * of <code>java.awt.Stroke</code>.
     */
    public static final String FILL_STROKE_KEY = "fill_stroke";
    /**
     * The attribute key used to determine which composite to use for the shape's interior. The value must be an
     * instance of <code>java.awt.Composite</code>.
     */
    public static final String FILL_COMPOSITE_KEY = "fill_composite";

    /**
     * The attribute key used to determine whether or not this figure has an out-line. The value must be an instance of
     * <code>java.lang.Boolean</code>.
     */
    public static final String OUTLINED_KEY = "outline";
    /**
     * The attribute key used to determine which fill pattern to use for the out-line. The value must be an instance of
     * <code>java.awt.Paint</code>.
     */
    public static final String OUTL_COLOR_KEY = "outl_color";
    /**
     * The attribute key used to determine which stroke to use for the out-line. The value must be an instance of
     * <code>java.awt.Stroke</code>.
     */
    public static final String OUTL_STROKE_KEY = "outl_stroke";
    /**
     * The attribute key used to determine which composite to use for the out-line. The value must be an instance of
     * <code>java.awt.Composite</code>.
     */
    public static final String OUTL_COMPOSITE_KEY = "outl_composite";

    /**
     * Gets the figure's center.
     */
    Point2D getCenterPoint();

    /**
     * Gets the bounding box of the figure
     */
    Rectangle2D getBounds();

    /**
     * Gets a shape representation of this figure.
     * <p>If the figure does not have a shape represenation, the method returns <code>null</code>.
     *
     * @return a shape representation of this figure or <code>null</code> if no such exists.
     */
    Shape getShape();

    /**
     * Returns the handles used to manipulate the figure. <code>createHandles</code> is a Factory Method for creating
     * handle objects.
     *
     * @return an array of handles
     *
     * @see FigureHandle
     */
    FigureHandle[] createHandles();

    /**
     * Returns an Enumeration of the figures contained in this figure
     */
    Figure[] getFigures();

    /**
     * Returns the figure that contains the given point.
     */
    Figure findFigureInside(double x, double y);

    /**
     * Checks if a point is inside the figure.
     */
    boolean containsPoint(double x, double y);

    /**
     * Checks whether the given figure is contained in this figure.
     */
    boolean includes(Figure figure);

    /**
     * Decomposes a figure into its parts. A figure is considered as a part of itself.
     */
    Figure[] decompose();

    /**
     * Determines whether figure is a (one-dimensional) line in a two-dimensional space.
     */
    boolean isOneDimensional();

    /**
     * Gets the figure as an area. One-dimensional figures are returned as line strokes with a width of 1 unit.
     */
    Area getAsArea();

    /**
     * Releases a figure's resources. Release is called when a figure is removed from a drawing. Informs the listeners
     * that the figure is removed by calling figureRemoved.
     */
    void dispose();

//    /**
//     * Invalidates the figure. This method informs its listeners
//     * that its current display box is invalid and should be
//     * refreshed.
//     */
//    void invalidate();
//
//    /**
//     * Informes that a figure is about to change such that its
//     * display box is affected.
//     * Here is an example of how it is used together with changed()
//     * <pre>
//     * public void setLocation(int x, int y) {
//     *      willChange();
//     *      // change the figure's location
//     *      changed();
//     *  }
//     * </pre>
//     * @see #invalidate
//     * @see #changed
//     */
//    void willChange();
//
//    /**
//     * Informes that a figure has changed its display box.
//     * This method also triggers an update call for its
//     * registered observers.
//     * @see #invalidate
//     * @see #willChange
//     *
//     */
//    void changed();

    /**
     * Gets the z value (back-to-front ordering) of this figure. Z values are not guaranteed to not skip numbers.
     */
    int getZValue();

    /**
     * Sets the z value (back-to-front ordering) of this figure. Z values are not guaranteed to not skip numbers.
     */
    void setZValue(int zValue);

    /**
     * Returns the attributes of this figure as a <code>Map</code>.
     */
    Map<String, Object> getAttributes();

    /**
     * Returns the named attribute or null if a a figure doesn't have an attribute. All figures support the attribute
     * names FillColor and FrameColor
     */
    Object getAttribute(String name);

    /**
     * Sets the named attribute to the new value
     */
    void setAttribute(String name, Object value);

    /**
     * Sets multiple attributes
     */
    void setAttributes(Map<String, Object> attributes);

    /**
     * Adds a listener for this figure.
     *
     * @param listener the listener to be added
     */
    void addFigureChangeListener(FigureChangeListener listener);

    /**
     * Removes a listener for this figure.
     *
     * @param listener the listener to be removed
     */
    void removeFigureChangeListener(FigureChangeListener listener);

    /**
     * Returns a Clone of this figure
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Figure clone();
}
