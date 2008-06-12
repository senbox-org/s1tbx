/*
 * $Id: DrawingEditor.java,v 1.1 2006/10/10 14:47:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.tool;

import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.draw.Figure;

/**
 * An editor which is used by tools to draw themselfes and to let them add, remove and query figures.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface DrawingEditor {

    /**
     * Returns the current tool for this drawing.
     */
    Tool getTool();

    /**
     * Sets the current tool for this drawing.
     */
    void setTool(Tool tool);

    /**
     * Repaints the current tool (if any).
     */
    void repaintTool();

    /**
     * Repaints the drawing.
     */
    void repaint();

    /**
     * Displays a status message. If <code>null</code> is passed to this method, the status message is reset or
     * cleared.
     *
     * @param message the message to be displayed
     */
    void setStatusMessage(String message);

    /**
     * Adds a new figure to the drawing.
     */
    void addFigure(Figure figure);

    /**
     * Removes a figure from the drawing.
     */
    void removeFigure(Figure figure);

    /**
     * Returns the number of figures.
     */
    int getNumFigures();

    /**
     * Gets the figure at the specified index.
     *
     * @return the figure, never <code>null</code>
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
