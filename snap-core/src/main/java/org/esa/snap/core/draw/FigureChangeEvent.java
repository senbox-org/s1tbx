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

import java.awt.Rectangle;
import java.util.EventObject;

/**
 * <code>FigureChangeEvent</code>s are passed to <code>FigureChangeListeners</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
* @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public class FigureChangeEvent extends EventObject {

    private static final Rectangle _EMPTY_RECT = new Rectangle(0, 0, 0, 0);
    private final Rectangle _rectangle;

    /**
     * Constructs an event for the given source figure.
     *
     * @param source the source figure
     */
    public FigureChangeEvent(Figure source) {
        super(source);
        _rectangle = _EMPTY_RECT;
    }

    /**
     * Constructs an event for the given source figure. The rectangle is the area to be invalvidated.
     *
     * @param source    the source figure
     * @param rectangle the rectangle is the area to be invalvidated
     */
    public FigureChangeEvent(Figure source, Rectangle rectangle) {
        super(source);
        _rectangle = new Rectangle(rectangle);
    }

    /**
     * Gets the changed figure
     */
    public Figure getFigure() {
        return (Figure) getSource();
    }

    /**
     * Gets the changed rectangle
     */
    public Rectangle getInvalidatedRectangle() {
        return _rectangle;
    }
}
