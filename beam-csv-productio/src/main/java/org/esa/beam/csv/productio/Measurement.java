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

package org.esa.beam.csv.productio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.Arrays;

/**
 * A default implementation of {@link Record} which holds all information of a measurement.
 */
public class Measurement implements Record {

    private final ProductData.UTC time;
    private final GeoPos geoPos;
    private final Object[] values;
    private final String locationName;

    public Measurement(String locationName, ProductData.UTC time, GeoPos geoPos, Object[] values) {
        this.locationName = locationName;
        this.time = time;
        this.geoPos = geoPos;
        this.values = new Object[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Measurement that = (Measurement) o;

        if (geoPos != null ? !geoPos.equals(that.geoPos) : that.geoPos != null) {
            return false;
        }
        if (locationName != null ? !locationName.equals(that.locationName) : that.locationName != null) {
            return false;
        }
        if (time != null ? !time.equals(that.time) : that.time != null) {
            return false;
        }
        if (!Arrays.equals(values, that.values)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = time != null ? time.hashCode() : 0;
        result = 31 * result + (geoPos != null ? geoPos.hashCode() : 0);
        result = 31 * result + (values != null ? Arrays.hashCode(values) : 0);
        result = 31 * result + (locationName != null ? locationName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Measurement{" +
               "geoPos=" + geoPos +
               ", time=" + time +
               ", values=" + (values == null ? null : Arrays.asList(values)) +
               ", locationName='" + locationName + '\'' +
               '}';
    }

    @Override
    public GeoPos getLocation() {
        return geoPos;
    }

    @Override
    public ProductData.UTC getTime() {
        return time;
    }

    @Override
    public Object[] getAttributeValues() {
        return values;
    }

    @Override
    public String getLocationName() {
        return locationName;
    }
}