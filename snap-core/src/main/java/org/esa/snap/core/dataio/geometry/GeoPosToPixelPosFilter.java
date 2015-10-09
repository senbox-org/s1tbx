/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataio.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

/**
*
* @author Olaf Danne
* @author Thomas Storm
*/
class GeoPosToPixelPosFilter implements CoordinateSequenceFilter {

    public int count = 0;
    private int numCoordinates;
    private GeoCoding geoCoding;

    public GeoPosToPixelPosFilter(int numCoordinates, GeoCoding geoCoding) {
        this.numCoordinates = numCoordinates;
        this.geoCoding = geoCoding;
    }

    @Override
    public void filter(CoordinateSequence seq, int i) {
        Coordinate coord = seq.getCoordinate(i);
        PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(coord.y, coord.x), null);
        // rounding needed because closed geometries yield errors if their first and last coordinate
        // do not exactly match
        double x = Math.round(pixelPos.x * 10000) / 10000;
        double y = Math.round(pixelPos.y * 10000) / 10000;
        coord.setCoordinate(new Coordinate(x, y));
        count++;
    }

    @Override
    public boolean isDone() {
        return numCoordinates == count;
    }

    @Override
    public boolean isGeometryChanged() {
        return numCoordinates == count;
    }
}
