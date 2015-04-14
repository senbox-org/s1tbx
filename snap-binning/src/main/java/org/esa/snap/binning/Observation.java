/*
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

package org.esa.snap.binning;

/**
 * An observation comprises a number of measurements at a certain point in time and space.
 *
 * @author Norman Fomferra
 */
public interface Observation extends Vector {
    /**
     * @return The time of this observation given as Modified Julian Day (MJD).
     */
    double getMJD();

    /**
     * @return The geographical latitude of this observation given as WGS-84 coordinate.
     */
    double getLatitude();

    /**
     * @return The geographical longitude of this observation given as WGS-84 coordinate.
     */
    double getLongitude();
}
