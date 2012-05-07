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
package org.esa.beam.framework.ui.tool;

import org.esa.beam.framework.draw.Figure;

import java.awt.Rectangle;

/**
 * An editor which is used by tools to draw themselfes and to let them add, remove and query figures.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public interface DrawingEditor {

    enum InsertMode {

        REPLACE,
        ADD,
        SUBTRACT
    }

    /**
     * @return The current tool for this drawing.
     */
    Tool getTool();

    /**
     * @param tool The current tool for this drawing.
     */
    void setTool(Tool tool);

    /**
     * @return A special "select" tool, may be null.
     */
    AbstractTool getSelectTool();

    /**
     * Called if a selection occurred.
     *
     * @param rectangle The selection rectangle in view coordinates.
     */
    void handleSelection(Rectangle rectangle);


    /**
     * Repaints the current tool (if any).
     */
    void repaintTool();

    /**
     * Repaints the drawing.
     */
    void repaint();

    /**
     * Adds a new figure to the drawing and replaces any existing figure.
     *
     * @param figure The figure to add to the this drawing.
     *
     * @deprecated since BEAM 4.6, use {@link #insertFigure(Figure, InsertMode)} instead
     */
    @Deprecated
    void addFigure(Figure figure);

    /**
     * Adds a new figure to the drawing using the specified {@link InsertMode insertion mode}.
     *
     * @param figure     The figure to add to this drawing.
     * @param insertMode Specifies how to add the figure to this drawing.
     */
    void insertFigure(Figure figure, InsertMode insertMode);

    /**
     * Removes a figure from the drawing.
     *
     * @param figure The figure to remove from this drawing.
     */
    void removeFigure(Figure figure);

    /**
     * Returns the number of figures.
     *
     * @return The number of figures
     */
    int getNumFigures();

    /**
     * Gets the figure at the specified index.
     *
     * @param index The index of the figure to retrieve.
     *
     * @return The figure, never <code>null</code>
     */
    Figure getFigureAt(int index);

    /**
     * Gets all figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    Figure[] getAllFigures();

    /**
     * Gets all selected figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    Figure[] getSelectedFigures();

    /**
     * Gets all figures having an attribute with the given name.
     *
     * @param name the attribute name
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    Figure[] getFiguresWithAttribute(String name);

    /**
     * Gets all figures having an attribute with the given name and value.
     *
     * @param name  the attribute name, must not be <code>null</code>
     * @param value the attribute value, must not be <code>null</code>
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    Figure[] getFiguresWithAttribute(String name, Object value);

}
