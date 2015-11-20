/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.grender;

import java.awt.Graphics2D;


/**
 * A rendering is used to render graphical data representations to a GUI widget, image or another
 * output device such as a printer.
 *
 * @author Norman Fomferra
 */
public interface Rendering {
    /**
     * @return The graphics context associated with this rendering.
     */
    Graphics2D getGraphics();

    /**
     * @return The porthole through which the model is viewed.
     */
    Viewport getViewport();
}
