/*
 * $Id$
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package com.bc.ceres.glayer.swing;

import com.bc.ceres.grender.Viewport;

import java.awt.geom.Rectangle2D;


/**
 * {@link javax.swing.JComponent JComponent}s implementing this interface are views which can
 * be adjusted using the {@link ViewportScrollPane}.
 */
public interface AdjustableView {
    /**
     * @return The viewport.
     */
    Viewport getViewport();

    /**
     * @return The viewable area in model coordinates.
     */
    Rectangle2D getModelBounds();
}
