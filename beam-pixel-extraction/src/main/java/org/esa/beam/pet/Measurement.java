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

package org.esa.beam.pet;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;

class Measurement {

    private final ProductData.UTC startTime;
    private GeoPos geoPos;
    private final double[] values;
    private int coordinateID;
    private String coordinateName;
    private boolean isValid;

    Measurement(int coordinateID, String name, ProductData.UTC time, GeoPos geoPos, double[] values, boolean isValid) {
        this.coordinateID = coordinateID;
        coordinateName = name;
        this.startTime = time;
        this.geoPos = geoPos;
        this.values = new double[values.length];
        this.isValid = isValid;
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    ProductData.UTC getStartTime() {
        return startTime;
    }

    double[] getValues() {
        return values;
    }

    float getLat() {
        return geoPos.lat;
    }

    float getLon() {
        return geoPos.lon;
    }

    int getCoordinateID() {
        return coordinateID;
    }

    String getCoordinateName() {
        return coordinateName;
    }

    boolean isValid() {
        return isValid;
    }
}