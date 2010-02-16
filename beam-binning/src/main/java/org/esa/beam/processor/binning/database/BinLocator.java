/*
 * $Id: BinLocator.java,v 1.1 2006/09/11 10:47:32 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.processor.binning.database;

import java.awt.Point;

import org.esa.beam.framework.datamodel.GeoPos;

//@todo 1 se/tb - class documentation

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
