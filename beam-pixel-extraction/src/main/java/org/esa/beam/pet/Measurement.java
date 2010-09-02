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

    private final String rasterName;
    private final ProductData.UTC startTime;
    private GeoPos geoPos;
    private final double[] values;

    Measurement(String rasterName, ProductData.UTC time, GeoPos geoPos, double[] values) {
        this.rasterName = rasterName;
        this.startTime = time;
        this.geoPos = geoPos;
        this.values = new double[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public String getRasterName() {
        return rasterName;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }

    public double[] getValues() {
        return values;
    }

    public float getLat() {
        return geoPos.lat;
    }

    public float getLon() {
        return geoPos.lon;
    }

}