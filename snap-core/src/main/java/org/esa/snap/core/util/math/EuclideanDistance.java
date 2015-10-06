package org.esa.snap.core.util.math;/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

/**
 * Distance measure based on the Euclidean 'flat' distance. In fact the square
 * of the Euclidean distance is used to avoid computation of the square root.
 *
 * @author Ralf Quast
 */
public class EuclideanDistance implements DistanceMeasure {

    private final double lon0;
    private final double lat0;

    public EuclideanDistance(double lon0, double lat0) {
        this.lon0 = lon0;
        this.lat0 = lat0;
    }

    @Override
    public double distance(double lon, double lat) {
        double deltaLat = lat - lat0;
        double deltaLon = lon - lon0;
        if (deltaLon < 0.0) {
            deltaLon = -deltaLon;
        }
        if (deltaLon > 180.0) {
            deltaLon = 360.0 - deltaLon;
        }
        return deltaLat * deltaLat + deltaLon * deltaLon;
    }
}
