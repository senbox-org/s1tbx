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

import java.util.EventListener;

/**
 * A listener interested in figure changes.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public interface FigureChangeListener extends EventListener {

    /**
     * Sent when an area is invalid
     */
    void figureInvalidated(FigureChangeEvent e);

    /**
     * Sent when a figure changed
     */
    void figureChanged(FigureChangeEvent e);

    /**
     * Sent when a figure was removed
     */
    void figureRemoved(FigureChangeEvent e);

    /**
     * Sent when requesting to remove a figure.
     */
    void figureRequestRemove(FigureChangeEvent e);

    /**
     * Sent when an update should happen.
     */
    void figureRequestUpdate(FigureChangeEvent e);
}
