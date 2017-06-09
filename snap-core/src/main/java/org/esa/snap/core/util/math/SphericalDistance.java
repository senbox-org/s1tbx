package org.esa.snap.core.util.math;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * This class computes the spherical distance (in Radian) between two (lon, lat) points.
 *
 * @author Ralf Quast
 */
public final class SphericalDistance implements DistanceMeasure {

    private final double lon;
    private final double lat;
    private final double si;
    private final double co;

    /**
     * Creates a new instance of this class.
     *
     * @param lon The reference longitude of this distance calculator.
     * @param lat The reference latitude of this distance calculator.
     */
    public SphericalDistance(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
        this.si = Math.sin(Math.toRadians(lat));
        this.co = Math.cos(Math.toRadians(lat));
    }

    /**
     * Returns the spherical distance (in Radian) of a given (lon, lat) point to
     * the reference (lon, lat) point.
     *
     * @param lon The longitude.
     * @param lat The latitude.
     *
     * @return the spherical distance (in Radian) of the given (lon, lat) point
     * to the reference (lon, lat) point.
     */
    @Override
    public double distance(double lon, double lat) {
        if (lon == this.lon && lat == this.lat) {
            return 0.0;
        }
        final double phi = Math.toRadians(lat);
        return Math.acos(si * Math.sin(phi) + co * Math.cos(phi) * Math.cos(Math.toRadians(lon - this.lon)));
    }
}
