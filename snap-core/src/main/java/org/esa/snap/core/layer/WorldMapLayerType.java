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

package org.esa.snap.core.layer;

import com.bc.ceres.glayer.support.ImageLayer;

/**
 * A layer used to display world maps.
 *
 * @author Marco Zühlke
 * @author Thomas Storm
 * @since BEAM 4.8
 */
public abstract class WorldMapLayerType extends ImageLayer.Type {

    /**
     * Returns the human readable label for the layer type
     *
     * @return a label
     */
    public abstract String getLabel();

}
