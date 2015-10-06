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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * A handle of a figure. Handles are used to move, rescale or rotate figures in a drawing.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public interface FigureHandle extends Drawable {

    public static final int HANDLESIZE = 8;

    /**
     * Locates the handle on the figure. The handle is drawn centered around the returned point.
     */
    public Point2D getCenterPoint();

    /**
     * Gets the display box of the handle.
     */
    public Rectangle2D getBounds();

    /**
     * Tracks a step of the interaction.
     *
     * @param x       the current x position
     * @param y       the current y position
     * @param anchorX the x position where the interaction started
     * @param anchorY the y position where the interaction started
     */
    public void invokeStep(int x, int y, int anchorX, int anchorY);

    /**
     * Tracks the end of the interaction.
     *
     * @param x       the current x position
     * @param y       the current y position
     * @param anchorX the x position where the interaction started
     * @param anchorY the y position where the interaction started
     */
    public void invokeEnd(int x, int y, int anchorX, int anchorY);

    /**
     * Gets the handle's owner.
     */
    public Figure getOwner();

    /**
     * Tests if a point is contained in the handle.
     */
    public boolean containsPoint(int x, int y);

//	public Undoable getUndoActivity();
//
//	public void setUndoActivity(Undoable newUndoableActivity);
}
