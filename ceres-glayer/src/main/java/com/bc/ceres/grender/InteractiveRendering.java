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

import java.awt.*;


/**
 * An interactive rendering is used to render graphical data representations on a GUI widget, allowing for
 * rendering of invalidated regions.
 *
 * @author Norman Fomferra
 */
public interface InteractiveRendering extends Rendering {

    /**
     * Invalidates the given view region so that it becomes
     * repainted as soon as possible.
     *
     * @param region The region to be invalidated (in view coordinates).
     */
    void invalidateRegion(Rectangle region);

    /**
     * Runs the given task in the thread that is used by the GUI library.
     * In <i>Swing</i>, this would be the <i>Event Dispatcher Thread</i> (EDT).
     *
     * @param task The task to be invoked.
     */
    void invokeLater(Runnable task);
}