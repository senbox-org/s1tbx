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
import java.util.Map;

/**
 * A shape figure that represents a two-dimensional area.
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public class AreaFigure extends ShapeFigure {

    public AreaFigure(Shape shape, Map<String, Object> attributes) {
        super(shape, false, attributes);
    }
}
