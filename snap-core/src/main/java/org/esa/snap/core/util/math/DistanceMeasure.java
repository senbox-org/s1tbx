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
 * Interface for measuring the distance of a (lon, lat) point to a reference
 * (lon, lat) point, which is defined by the implementing class.
 * <p>
 * Let p and q denote two points en the Earth. Then, loosely speaking, any
 * distance measure d(p, q) has to satisfy the following properties:
 * <p>
 * (1) d(p, q) = 0, if p = q
 * <p>
 * (2) d(p, q) &gt; 0, if p ≠ q
 *
 * @author Ralf Quast
 * @since Version 5.0
 */
public interface DistanceMeasure {

    /**
     * Returns the distance of a given (lon, lat) point to the reference (lon, lat) point.
     *
     * @param lon The longitude.
     * @param lat The latitude.
     *
     * @return the distance.
     */
    double distance(double lon, double lat);
}
