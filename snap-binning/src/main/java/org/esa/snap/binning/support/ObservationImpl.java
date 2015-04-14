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

package org.esa.snap.binning.support;

import org.esa.snap.binning.Observation;

/**
 * A default implementation of the {@link org.esa.snap.binning.Observation} interface.
 *
 * @author Norman Fomferra
 */
public final class ObservationImpl implements Observation {

    private final double latitude;
    private final double longitude;
    private final double mjd;
    private final float[] measurements;

    public ObservationImpl(double latitude, double longitude, double mjd, float... measurements) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.mjd = mjd;
        this.measurements = measurements;
    }

    @Override
    public double getMJD() {
        return mjd;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public int size() {
        return measurements.length;
    }

    @Override
    public float get(int varIndex) {
        return measurements[varIndex];
    }
}
