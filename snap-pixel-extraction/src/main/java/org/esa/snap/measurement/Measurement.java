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

package org.esa.snap.measurement;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ObjectUtils;

import java.util.Arrays;

/**
 * A container class which holds all information of a measurement.
 */
public class Measurement {

    private final ProductData.UTC time;
    private final GeoPos geoPos;
    private final Object[] values;
    private final int coordinateID;
    private final long productId;
    private final double pixelX;
    private final double pixelY;
    private final String coordinateName;
    private final boolean isValid;
    private final String[] originalAttributeNames;

    public Measurement(int coordinateID, String name, long productId, double pixelX, double pixelY, ProductData.UTC time,
                       GeoPos geoPos, Object[] values, boolean isValid) {
        this(coordinateID, name, productId, pixelX, pixelY, time, geoPos, values, null, isValid);
    }

    public Measurement(int coordinateID, String name, long productId, double pixelX, double pixelY, ProductData.UTC time,
                       GeoPos geoPos, Object[] values, String[] originalAttributeNames, boolean isValid) {
        this.coordinateID = coordinateID;
        this.productId = productId;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        coordinateName = name;
        this.time = time;
        this.geoPos = geoPos;
        this.isValid = isValid;
        this.values = new Object[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        this.originalAttributeNames = originalAttributeNames;
    }

    public ProductData.UTC getTime() {
        return time;
    }

    public Object[] getValues() {
        return values;
    }

    public double getLat() {
        return geoPos.lat;
    }

    public double getLon() {
        return geoPos.lon;
    }

    public int getCoordinateID() {
        return coordinateID;
    }

    public String getCoordinateName() {
        return coordinateName;
    }

    public boolean isValid() {
        return isValid;
    }

    public double getPixelX() {
        return pixelX;
    }

    public double getPixelY() {
        return pixelY;
    }

    public long getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Measurement that = (Measurement) o;

        if (coordinateID != that.coordinateID) {
            return false;
        }
        if (isValid != that.isValid) {
            return false;
        }
        if (Double.compare(that.pixelX, pixelX) != 0) {
            return false;
        }
        if (Double.compare(that.pixelY, pixelY) != 0) {
            return false;
        }
        if (productId != that.productId) {
            return false;
        }
        if (!coordinateName.equals(that.coordinateName)) {
            return false;
        }
        if (!geoPos.equals(that.geoPos)) {
            return false;
        }
        if (!ObjectUtils.equalObjects(time, that.time)) {
            return false;
        }
        if (!Arrays.equals(values, that.values)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        if (time != null) {
            result = time.hashCode();
        } else {
            result = 139651030;
        }
        result = 31 * result + geoPos.hashCode();
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + coordinateID;
        result = 31 * result + (int) productId;
        result = 31 * result + (pixelX == +0.0f ? 0 : (int)Double.doubleToLongBits(pixelX));
        result = 31 * result + (pixelY == +0.0f ? 0 : (int)Double.doubleToLongBits(pixelY));
        result = 31 * result + coordinateName.hashCode();
        result = 31 * result + (isValid ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "time=" + time +
                ", geoPos=" + geoPos +
                ", values=" + (values == null ? null : Arrays.asList(values)) +
                ", coordinateID=" + coordinateID +
                ", productId=" + productId +
                ", pixelX=" + pixelX +
                ", pixelY=" + pixelY +
                ", coordinateName='" + coordinateName + '\'' +
                ", isValid=" + isValid +
                '}';
    }

    public String[] getOriginalAttributeNames() {
        return originalAttributeNames;
    }
}
