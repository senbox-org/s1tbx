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
package org.esa.beam.processor.binning;

import org.esa.beam.processor.binning.database.BinDatabaseConstants;

@Deprecated
/**
 * Class specialised in handling a latlon equi-rectangular non-projected raster
 * map.
 *
 * @author Thomas Lankester, Infoterra Ltd.
 * @version 0.10, 21/05/03
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3PlateCarreRaster extends L3ProjectionRaster {

    /**
     * Constructs the object with default values
     */
    public L3PlateCarreRaster() {
    }

    /**
     * Returns the width of the projection grid
     */
    @Override
    public int getWidth() {
        return _colMax - _colMin;
    }

    /**
     * Returns the height of the projection grid
     */
    @Override
    public int getHeight() {
        return _rowMax - _rowMin;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Calculates the grid step in decimal degrees.
     *
     * @param cellSize the grid cell size in kilometers
     */
    @Override
    protected void calculateStepSize(float cellSize) {
        double kmPerDegree = BinDatabaseConstants.PI_EARTH_RADIUS / 180.f;
        int steps = (int) Math.round(kmPerDegree / cellSize);
        _degreesPerStep = 1.f / steps;
        _stepsPerDegree = 1.f / _degreesPerStep;
    }
}
