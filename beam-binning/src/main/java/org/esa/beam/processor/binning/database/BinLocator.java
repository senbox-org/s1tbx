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
package org.esa.beam.processor.binning.database;

import org.esa.beam.framework.datamodel.GeoPos;

import java.awt.Point;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public interface BinLocator {

    /**
     * Retrieves the latitude and longitude of the bin at the given index.
     */
    GeoPos getLatLon(int index, GeoPos recycle);

    /**
     * Retrieves the bin index for a given latitude and longitude
     */
    int getIndex(GeoPos latlon);

    /**
     * Retrieves latitude and longitude for a given row/column pair
     */
    GeoPos getLatLon(Point rowcol, GeoPos recycle);

    /**
     * Retrieves the row / column pair for a given lat/lon pair
     */
    Point getRowCol(GeoPos latlon, Point recycle);

    /**
     * Transforms a two dimensional grid coordinate to a one dimensional
     *  method name corrected T Lankester 26/04/05
     */
    int rowColToIndex(Point rowcol);

    /**
     * Returns whether the desired position is valid in the context of this grid implementation
     */
    boolean isValidPosition(Point rowcol);

    /**
     * Returns the number of cells of the locator grid.
     */
    int getNumCells();

    /**
     * Returns the width of the locator grid.
     */
    int getWidth();

    /**
     * Returns the height of the locator grid
     */
    int getHeight();
}
